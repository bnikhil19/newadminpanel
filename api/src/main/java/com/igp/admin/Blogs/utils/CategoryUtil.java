package com.igp.admin.Blogs.utils;

import com.igp.admin.Blogs.models.BlogResultModel;
import com.igp.admin.Blogs.models.CategoryModel;
import com.igp.admin.Blogs.models.CategorySubCategoryModel;
import com.igp.admin.Blogs.models.SeoBlogModel;
import com.igp.config.instance.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suditi on 3/5/18.
 */
public class CategoryUtil {
    private static final Logger logger = LoggerFactory.getLogger(CategoryUtil.class);

    public BlogResultModel createCategory(CategoryModel categoryModel){
        Connection connection = null;
        String statement;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet =  null;
        BlogsUtil blogsUtil = new BlogsUtil();
        BlogResultModel blogResultModel = new BlogResultModel();
        try{
            boolean validUrl = blogsUtil.checkUrlWithNoSpecialChar(categoryModel.getUrl());
            if(validUrl==false){
                blogResultModel.setError(true);
                blogResultModel.setMessage("Invalid URL");
                return blogResultModel;
            }
            BlogResultModel validUrlResult = validateCategoryUrl(categoryModel.getFkAssociateId(), categoryModel.getUrl());
            if(validUrlResult.isError() && validUrlResult.getMessage().equalsIgnoreCase("urlexist")){
                blogResultModel.setError(true);
                blogResultModel.setMessage("Please choose different url for category");
                return blogResultModel;
            }
            if(categoryModel.getParentId() != 0){
                BlogResultModel parentExistResult = validateParentCategory(categoryModel.getParentId());
                if(!parentExistResult.getMessage().equalsIgnoreCase("categoryexist")){
                    blogResultModel.setError(true);
                    blogResultModel.setMessage("Invalid parent category");
                    return blogResultModel;
                }
            }
            connection = Database.INSTANCE.getReadWriteConnection();
            /*statement="INSERT INTO blog_categories (categories_name,categories_name_alt,parent_id,sort_order,categories_description,categories_name_for_url," +
                "categories_meta_title,categories_meta_keywords,categories_meta_description,categories_introduction_text,categories_introduction_down_text,categories_status) "
                + " VALUES (? ,? ,? ,? ,? ,? ,? ,? ,? ,? ,? ,?)";*/
            statement="INSERT INTO blog_categories (fk_associate_id, categories_name,parent_id,sort_order,categories_name_for_url," +
                "categories_meta_title,categories_meta_keywords,categories_meta_description,status) "
                + " VALUES (? ,? ,? ,? ,? ,? ,? ,? ,? )";
            preparedStatement = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, categoryModel.getFkAssociateId());
            preparedStatement.setString(2, categoryModel.getTitle());
            preparedStatement.setInt(3, categoryModel.getParentId());
            preparedStatement.setInt(4, categoryModel.getSortOrder());
            preparedStatement.setString(5, categoryModel.getUrl());
            preparedStatement.setString(6, categoryModel.getSeoModel().getSeoTitle());
            preparedStatement.setString(7, categoryModel.getSeoModel().getSeoKeywords());
            preparedStatement.setString(8, categoryModel.getSeoModel().getSeoDescription());
            preparedStatement.setInt(9, 1);
            logger.debug("preparedstatement of insert blog_categories : "+preparedStatement);

            Integer status = preparedStatement.executeUpdate();
            if (status == 0) {
                logger.error("Failed to create Category post");
            } else
            {
                resultSet = preparedStatement.getGeneratedKeys();
                resultSet.first();
                categoryModel.setId(resultSet.getInt(1));
                blogResultModel.setObject(categoryModel.getId());
                logger.debug("New Category created with url : "+categoryModel.getUrl()+" id : "+categoryModel.getId());
            }
        }catch (Exception exception){
            logger.debug("error occured while creating Category : ",exception);
            blogResultModel.setError(true);
            blogResultModel.setMessage("error in insertion.");
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
            Database.INSTANCE.closeResultSet(resultSet);
        }
        return blogResultModel;
    }
    public BlogResultModel updateCategory(CategoryModel categoryModel){
        boolean result = true;
        Connection connection = null;
        String statement;
        PreparedStatement preparedStatement = null;
        BlogsUtil blogsUtil = new BlogsUtil();
        BlogResultModel blogResultModel = new BlogResultModel();
        try{

            boolean validUrl = blogsUtil.checkUrlWithNoSpecialChar(categoryModel.getUrl());
            if(validUrl==false){
                blogResultModel.setError(true);
                blogResultModel.setMessage("Invalid URL.");
                return blogResultModel;
            }
            if(categoryModel.getParentId() != 0){
                if (categoryModel.getParentId() == categoryModel.getId()) {
                    blogResultModel.setError(true);
                    blogResultModel.setMessage("Please choose different parent category");
                    return blogResultModel;
                }
                BlogResultModel parentExistResult = validateParentCategory(categoryModel.getParentId());
                if(!parentExistResult.getMessage().equalsIgnoreCase("categoryexist")){
                    blogResultModel.setError(true);
                    blogResultModel.setMessage("Invalid parent category");
                    return blogResultModel;
                }
            }

            connection = Database.INSTANCE.getReadWriteConnection();
            statement="UPDATE blog_categories SET categories_name=?,parent_id=?,sort_order=?,categories_name_for_url=?," +
                "categories_meta_title=?,categories_meta_keywords=?,categories_meta_description=?,status=? WHERE categories_id = ?";
            preparedStatement = connection.prepareStatement(statement);
            preparedStatement = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, categoryModel.getTitle());
            preparedStatement.setInt(2, categoryModel.getParentId());
            preparedStatement.setInt(3, categoryModel.getSortOrder());
            preparedStatement.setString(4, categoryModel.getUrl());
            preparedStatement.setString(5, categoryModel.getSeoModel().getSeoTitle());
            preparedStatement.setString(6, categoryModel.getSeoModel().getSeoKeywords());
            preparedStatement.setString(7, categoryModel.getSeoModel().getSeoDescription());
            preparedStatement.setInt(8, categoryModel.getStatus());
            preparedStatement.setInt(9, categoryModel.getId());
            logger.debug("preparedstatement of update blog_categories : "+preparedStatement);

            Integer status = preparedStatement.executeUpdate();
            if (status == 0) {
                blogResultModel.setError(true);
                blogResultModel.setMessage("Failed to update Category");
                logger.error("Failed to update Category");
            } else {
                blogResultModel.setError(false);
                logger.debug("Category updated with id : "+categoryModel.getId());
            }

        }catch (Exception exception){
            blogResultModel.setError(true);
            blogResultModel.setMessage("error occured while updating Category");
            logger.debug("error occured while updating Category ",exception);
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
        }
        return blogResultModel;
    }

    //this will disable category & its sub categories if any (put status as 0)
    public boolean deleteCategory(int id){
        boolean result = false;
        Connection connection = null;
        String statement;
        PreparedStatement preparedStatement = null;
        try{
            connection = Database.INSTANCE.getReadWriteConnection();
            //statement="DELETE FROM blog_categories WHERE categories_id = ?";
            statement="update blog_categories set status = 0 WHERE categories_id = ? OR parent_id = ?";
            preparedStatement = connection.prepareStatement(statement);
            preparedStatement.setInt(1, id);
            preparedStatement.setInt(2, id);

            Integer status = preparedStatement.executeUpdate();
            if (status == 0) {
                logger.error("Failed to delete Category ");
            } else {
                result = true;
                logger.debug("Category deleted with id : "+id);
                logger.debug("Rows affected : "+status);
            }
        }catch (Exception exception){
            logger.debug("error occured while deleting Category ",exception);
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
        }
        return result;
    }
    public boolean validateCategory(int fkAssociateId, String categoryName, String subCategoryName){
        Connection connection = null;
        String statement="";
        ResultSet resultSet =  null;
        PreparedStatement preparedStatement = null;
        boolean  result = false;
        try{
            statement = "select bct.categories_id as p_cat, bct2.categories_id as ch_cat, bct.fk_associate_id, bct.categories_name as p_cat_name, bct2.categories_name as ch_cat_name "
                +" from blog_categories bct,blog_categories bct2"
                +" where bct.fk_associate_id = bct2.fk_associate_id AND bct.categories_id = bct2.parent_id AND bct.status=1 AND bct2.status=1"
                +" AND bct2.categories_name_for_url = ? AND bct.categories_name_for_url = ? AND bct.fk_associate_id = ?";
            connection = Database.INSTANCE.getReadOnlyConnection();
            preparedStatement = connection.prepareStatement(statement);
            preparedStatement.setString(1, subCategoryName);
            preparedStatement.setString(2, categoryName);
            preparedStatement.setInt(3, fkAssociateId);
            logger.debug("preparedstatement of finding valid category and subcategory : "+preparedStatement);

            resultSet = preparedStatement.executeQuery();
            if (resultSet.first()) {
                result = true;
            }

        }catch (Exception exception){
            logger.debug("error occured while finding valid category and subcategory",exception);
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
            Database.INSTANCE.closeResultSet(resultSet);
        }
        return result;
    }
    public List<CategoryModel> getCategoryList(int fkAssociateId, int startLimit, int endLimit){
        Connection connection = null;
        String statement="";
        ResultSet resultSet =  null;
        PreparedStatement preparedStatement = null;
        List<CategoryModel> categorySubCategoryLists = new ArrayList<>();
        try{
                statement="SELECT bmh.home_name,bct.categories_id AS cat_id, bct.categories_name AS cat_name, bct.categories_name_for_url AS cat_url," +
                    "  bct.parent_id, bct.sort_order,bct.categories_meta_title,bct.categories_meta_keywords,bct.categories_meta_description,bct.status,bct.fk_associate_id," +
                    " group_concat(bct2.categories_id,',', bct2.categories_name,',',bct2.categories_name_for_url,',',bct2.parent_id,',',bct2.sort_order,',',bct2.categories_meta_title,',',bct2.categories_meta_keywords,',',bct2.categories_meta_description,',',bct2.status,',',bct2.fk_associate_id separator ':') AS subcat" +
                    " FROM blog_categories bct LEFT JOIN blog_categories bct2 on bct.categories_id = " +
                    "bct2.parent_id and bct.fk_associate_id = " +
                    "bct2.fk_associate_id LEFT JOIN blog_meta_home bmh on bct.fk_associate_id = bmh.fk_associate_id WHERE bct.fk_associate_id = ? AND bct.parent_id = 0 GROUP BY " +
                    "bct.categories_id ORDER BY bct.sort_order limit ?,?";

            connection = Database.INSTANCE.getReadOnlyConnection();
            preparedStatement = connection.prepareStatement(statement);
            preparedStatement.setInt(1, fkAssociateId);
            preparedStatement.setInt(2, startLimit);
            preparedStatement.setInt(3, endLimit);

            logger.debug("preparedstatement of finding category list : "+preparedStatement);

            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                CategoryModel categorySubCategoryModel = new CategoryModel();
                String categoryStrArray[] = null;
                categorySubCategoryModel.setUrl(resultSet.getString("cat_url"));
                categorySubCategoryModel.setTitle(resultSet.getString("cat_name"));
                categorySubCategoryModel.setId(resultSet.getInt("cat_id"));

                categorySubCategoryModel.setParentId(resultSet.getInt("bct.parent_id"));
                categorySubCategoryModel.setSortOrder(resultSet.getInt("bct.sort_order"));
                categorySubCategoryModel.setStatus(resultSet.getInt("bct.status"));
                categorySubCategoryModel.setFkAssociateId(resultSet.getInt("bct.fk_associate_id"));
                categorySubCategoryModel.setFkAssociateName(resultSet.getString("bmh.home_name"));


                SeoBlogModel seoBlogModel = new SeoBlogModel();
                seoBlogModel.setSeoTitle(resultSet.getString("bct.categories_meta_title"));
                seoBlogModel.setSeoKeywords(resultSet.getString("bct.categories_meta_keywords"));
                seoBlogModel.setSeoDescription(resultSet.getString("bct.categories_meta_description"));
                categorySubCategoryModel.setSeoModel(seoBlogModel);

                String subcat = resultSet.getString("subcat");
                if(subcat!=null){
                    List<CategoryModel> subcategoryList = new ArrayList<>();
                    categoryStrArray = subcat.split(":");
                    for(int i = 0; i< categoryStrArray.length ; i++){
                        CategoryModel subcategory = new CategoryModel();
                        String[] dataArray = categoryStrArray[i].split(",");
                        subcategory.setId(new Integer(dataArray[0]));
                        subcategory.setTitle(dataArray[1]);
                        subcategory.setUrl(dataArray[2]);
                        subcategory.setParentId(Integer.parseInt(dataArray[3]));
                        subcategory.setSortOrder(Integer.parseInt(dataArray[4]));

                        SeoBlogModel seoBlogModel2 = new SeoBlogModel();
                        seoBlogModel2.setSeoTitle(dataArray[5]);
                        seoBlogModel2.setSeoKeywords(dataArray[6]);
                        seoBlogModel2.setSeoDescription(dataArray[7]);
                        subcategory.setSeoModel(seoBlogModel2);
                        subcategory.setStatus(Integer.parseInt(dataArray[8]));
                        subcategory.setFkAssociateId(Integer.parseInt(dataArray[9]));
                        subcategory.setFkAssociateName(resultSet.getString("bmh.home_name"));


                        subcategoryList.add(subcategory);
                    }
                    categorySubCategoryModel.setSubCategoryModelList(subcategoryList);
                }
                categorySubCategoryLists.add(categorySubCategoryModel);
            }

        }catch (Exception exception){
            logger.debug("error occured while getting category ",exception);
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
            Database.INSTANCE.closeResultSet(resultSet);
        }
        return categorySubCategoryLists;
    }

    public BlogResultModel validateCategoryUrl(int fkAssociateId, String url){
        Connection connection = null;
        String statement = "";
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        BlogResultModel result = new BlogResultModel();

        try{
            connection = Database.INSTANCE.getReadOnlyConnection();

            statement = "select * from blog_categories where fk_associate_id = "+fkAssociateId+" AND categories_name_for_url= '"+url+ "'";

            preparedStatement = connection.prepareStatement(statement);
            logger.debug("preparedStatement for validating categories_name_for_url -> " + preparedStatement);

            resultSet = preparedStatement.executeQuery();
            if(resultSet.first()){
                result.setError(true);
                result.setMessage("urlexist");
            }

        }catch (Exception e){
            logger.debug("error occured while validating url for category.", e);
            result.setError(true);
            result.setMessage(e.getMessage());
        }finally {
            Database.INSTANCE.closeStatement(preparedStatement);
            Database.INSTANCE.closeConnection(connection);
            Database.INSTANCE.closeResultSet(resultSet);
        }
        return result;
    }

    //this will check whether category exist or not
    public BlogResultModel validateParentCategory(int id){
        Connection connection = null;
        String statement = "";
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        BlogResultModel result = new BlogResultModel();

        try{
            connection = Database.INSTANCE.getReadOnlyConnection();

            statement = "select * from blog_categories where categories_id = '"+id+ "'";

            preparedStatement = connection.prepareStatement(statement);
            logger.debug("preparedStatement for url -> " + preparedStatement);

            resultSet = preparedStatement.executeQuery();
            if(resultSet.first()){
                result.setMessage("categoryexist");
            }

        }catch (Exception e){
            logger.debug("error occured while validating category.", e);
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
