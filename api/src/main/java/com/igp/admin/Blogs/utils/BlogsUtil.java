package com.igp.admin.Blogs.utils;

import com.igp.admin.Blogs.models.BlogMainModel;
import com.igp.admin.Blogs.models.CategorySubCategoryModel;
import com.igp.admin.Blogs.models.BlogResultModel;
import com.igp.config.instance.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by suditi on 2/5/18.
 */
public class BlogsUtil {
    private static final Logger logger = LoggerFactory.getLogger(BlogsUtil.class);

    public String createBlog(BlogMainModel blogMainModel){
        String url = "", tempUrl = "";
        Connection connection = null;
        String statement;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet =  null;
        try{
        //    tempUrl = createUrlUsingTitle(blogMainModel.getTitle());
            connection = Database.INSTANCE.getReadWriteConnection();
            statement="INSERT INTO blog_post (title,created_by,description,content,url,published_date," +
                "fk_associate_id,status,blog_meta_title,blog_meta_keywords,blog_meta_description,flag_featured,sort_order) "
                + " VALUES ( ? ,? ,? ,? ,? ,now() ,? ,? ,? ,? , ? ,? ,?)";
            preparedStatement = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, blogMainModel.getTitle());
            preparedStatement.setString(2, blogMainModel.getUser());
            preparedStatement.setString(3, blogMainModel.getShortDescription());
            preparedStatement.setString(4, blogMainModel.getDescription());
            preparedStatement.setString(5, blogMainModel.getUrl());
            preparedStatement.setInt(6, blogMainModel.getFkAssociateId());
            preparedStatement.setInt(7, blogMainModel.getStatus()); // by default keeping status as 1
            preparedStatement.setString(8, blogMainModel.getSeoModel().getSeoTitle());
            preparedStatement.setString(9, blogMainModel.getSeoModel().getSeoKeywords());
            preparedStatement.setString(10, blogMainModel.getSeoModel().getSeoDescription());
            preparedStatement.setInt(11, blogMainModel.getFlagFeatured()); // by default keeping flag_featured as 0
            preparedStatement.setInt(12, blogMainModel.getSortOrder()); // by default keeping sort order as 1

            logger.debug("preparedstatement of insert blog_post : "+preparedStatement);

            Integer status = preparedStatement.executeUpdate();
            if (status == 0) {
                logger.error("Failed to create blog post");
            } else
            {
                resultSet = preparedStatement.getGeneratedKeys();
                resultSet.first();
                blogMainModel.setId(resultSet.getInt(1));
                statement="INSERT INTO blog_post_image (blog_id,image_url,date_created,flag_featured) VALUES (? ,? ,now(),1)";
                preparedStatement = connection.prepareStatement(statement);
                preparedStatement.setInt(1, blogMainModel.getId());
                preparedStatement.setString(2, blogMainModel.getImageUrl());

                logger.debug("preparedstatement of insert blog_post_image : "+preparedStatement);

                status = preparedStatement.executeUpdate();
                if (status == 0) {
                    logger.error("Failed to create blog blog_post_image");
                }else {
                    List<Integer> list = new ArrayList<>();
                    for (Map.Entry<Integer, List<Integer>> entry : blogMainModel.getCategories().entrySet())
                    {
                        int i = entry.getKey();
                        list = entry.getValue();
                        list.add(i);
                        int size = list.size();
                        while (size>0) {
                            statement = "INSERT INTO blog_cat_map (blog_id,categories_id) VALUES (? ,?)";
                            preparedStatement = connection.prepareStatement(statement);
                            preparedStatement.setInt(1, blogMainModel.getId());
                            preparedStatement.setInt(2, list.get(size-1));
                            logger.debug("preparedstatement of insert blog_cat_map : " + preparedStatement);
                            status = preparedStatement.executeUpdate();
                            if (status == 0) {
                                logger.error("Failed to insert blog blog_cat_map");
                            }
                            size--;
                        }
                    }
                }
                url = blogMainModel.getUrl();
                logger.debug("New blog post created with url : "+url+" id : "+blogMainModel.getId());
            }
        }catch (Exception exception){
            logger.debug("error occured while creating blog post ",exception);
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
            Database.INSTANCE.closeResultSet(resultSet);
        }
        return url;
    }
    public boolean updateBlog(BlogMainModel blogMainModel){
        boolean result = false;
        Connection connection = null;
        String statement;
        PreparedStatement preparedStatement = null;
        try{
            connection = Database.INSTANCE.getReadWriteConnection();
            statement="UPDATE blog_post SET title = ?,created_by = ?,description = ?,content = ?,url = ?," +
                "fk_associate_id = ?,status = ?,blog_meta_title = ?,blog_meta_keywords = ?," +
                "blog_meta_description = ?,flag_featured = ?,sort_order = ?,published_date ? WHERE id = ?";
            preparedStatement = connection.prepareStatement(statement);
            preparedStatement.setString(1, blogMainModel.getTitle());
            preparedStatement.setString(2, blogMainModel.getUser());
            preparedStatement.setString(3, blogMainModel.getShortDescription());
            preparedStatement.setString(4, blogMainModel.getDescription());
            preparedStatement.setString(5, blogMainModel.getUrl());
            preparedStatement.setInt(6, blogMainModel.getFkAssociateId());
            preparedStatement.setInt(7, blogMainModel.getStatus());
            preparedStatement.setString(8, blogMainModel.getSeoModel().getSeoTitle());
            preparedStatement.setString(9, blogMainModel.getSeoModel().getSeoKeywords());
            preparedStatement.setString(10, blogMainModel.getSeoModel().getSeoDescription());
            preparedStatement.setInt(11, blogMainModel.getFlagFeatured());
            preparedStatement.setInt(12, blogMainModel.getSortOrder());
            preparedStatement.setString(13, blogMainModel.getPublishDate());
            preparedStatement.setInt(14, blogMainModel.getId());
            logger.debug("preparedstatement of update blog_post : "+preparedStatement);

            Integer status = preparedStatement.executeUpdate();
            if (status == 0) {
                logger.error("Failed to update blog post");
            } else {
                result = true;
                logger.debug("Blog post updated with id : "+blogMainModel.getId());
            }
        }catch (Exception exception){
            logger.debug("error occured while updating blog post ",exception);
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
        }
        return result;
    }
    public boolean deleteBlog(BlogMainModel blogMainModel){
        boolean result = false;
        Connection connection = null;
        String statement;
        PreparedStatement preparedStatement = null;
        try{
            connection = Database.INSTANCE.getReadWriteConnection();
            statement="DELETE FROM blog_post WHERE id = ?";
            preparedStatement = connection.prepareStatement(statement);
            preparedStatement.setInt(1, blogMainModel.getId());

            Integer status = preparedStatement.executeUpdate();
            if (status == 0) {
                logger.error("Failed to delete blog post");
            } else {
                result = true;
                statement="DELETE FROM blog_cat_map WHERE blog_id = ?";
                preparedStatement = connection.prepareStatement(statement);
                preparedStatement.setInt(1, blogMainModel.getId());
                status = preparedStatement.executeUpdate();
                if (status == 0) {
                    logger.error("Failed to delete blog post");
                } else {
                    result = true;
                    statement="DELETE FROM blog_post_image WHERE blog_id = ?";
                    preparedStatement = connection.prepareStatement(statement);
                    preparedStatement.setInt(1, blogMainModel.getId());

                    logger.debug("Blog post deleted from blog_post_imagewith id : "+blogMainModel.getId());
                }
                logger.debug("Blog post deleted with id : "+blogMainModel.getId());
            }
        }catch (Exception exception){
            logger.debug("error occured while deleting blog post ",exception);
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
        }
        return result;
    }


    public String createUrlUsingTitle(String title){
        String url = "";
        String correctStr = title.trim();
        try{
            String pattern1 = "^[0-9A-Za-z]*$";
            if (!title.matches(pattern1)) {
                logger.debug("url has some unmatched special char.Let's replace it with a hiphen.");
                Pattern pattern = Pattern.compile("^[0-9A-Za-z]*$");
                // pattern allows a set of special chars,apha-numeric replace it with hyphen
                int count = 0;
                int length = correctStr.length();
                int i = 0;
                while (i < length) {
                    Matcher m = pattern.matcher(title.charAt(i)+"");
                    if (!m.matches()) {
                        //  logger.debug("Unmatched character is : "+title.charAt(i)+" at index : "+i);
                        int index = i + 1 ;
                        count++;
                        correctStr = correctStr.substring(0, i) + "-" + correctStr.substring(index);
                        // replace the un matched char by a space.
                    }
                    i++;
                }
                url = correctStr;
                logger.debug("Finally the returned string from special char match is : "+url);
            }
        }catch (Exception e){
            logger.debug("error occured while creating url ",e);
        }
        return url;
    }

    //this method will return true if passed (fkAssociateId, url) combination already exist
    public BlogResultModel validateBlogUrl(int fkAssociateId, String url){
        Connection connection = null;
        String statement = "";
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        BlogResultModel result = new BlogResultModel();

        try{
            connection = Database.INSTANCE.getReadOnlyConnection();
            statement = "select * from blog_post where fk_associate_id = ? AND url= ?";
            preparedStatement = connection.prepareStatement(statement);
            preparedStatement.setInt(1, fkAssociateId);
            preparedStatement.setString(2, url);
            logger.debug("preparedStatement for validating blog url -> ", preparedStatement);

            resultSet = preparedStatement.executeQuery();
            if(resultSet.first()){
                result.setError(false);
                result.setMessage("urlexist");
            }

        }catch (Exception e){
            logger.debug("error occured while validating url for blog.", e);
            result.setError(true);
            result.setMessage(e.getMessage());
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
            Database.INSTANCE.closeResultSet(resultSet);
        }
        return result;
    }
}
