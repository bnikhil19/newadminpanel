package com.igp.admin.Blogs.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by suditi on 3/5/18.
 */
public class CategoryModel {

    @JsonProperty("id")
    private int id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("namealt")
    private String nameAlt;

    @JsonProperty("url")
    private String url;

    @JsonProperty("description")
    private String description;

    @JsonProperty("seo")
    private SeoBlogModel seoModel;

    @JsonProperty("parentid")
    private int parentId;

    @JsonProperty("sortorder")
    private int sortOrder;

    @JsonProperty("status")
    private int status;

    @JsonProperty("introtext")
    private String introText;

    @JsonProperty("introdowntext")
    private String introDownText;

    @JsonProperty("fkasid")
    private Integer fkAssociateId;

    @JsonProperty("fkasname")
    private String fkAssociateName;

    @JsonProperty("subcategory")
    private List<CategoryModel> subCategoryModelList;

    public List<CategoryModel> getSubCategoryModelList() {
        return subCategoryModelList;
    }

    public void setSubCategoryModelList(List<CategoryModel> subCategoryModelList) {
        this.subCategoryModelList = subCategoryModelList;
    }

    public Integer getFkAssociateId() {
        return fkAssociateId;
    }

    public void setFkAssociateId(Integer fkAssociateId) {
        this.fkAssociateId = fkAssociateId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNameAlt() {
        return nameAlt;
    }

    public void setNameAlt(String nameAlt) {
        this.nameAlt = nameAlt;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SeoBlogModel getSeoModel() {
        return seoModel;
    }

    public void setSeoModel(SeoBlogModel seoModel) {
        this.seoModel = seoModel;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getIntroText() {
        return introText;
    }

    public void setIntroText(String introText) {
        this.introText = introText;
    }

    public String getIntroDownText() {
        return introDownText;
    }

    public void setIntroDownText(String introDownText) {
        this.introDownText = introDownText;
    }

    public String getFkAssociateName() {
        return fkAssociateName;
    }

    public void setFkAssociateName(String fkAssociateName) {
        this.fkAssociateName = fkAssociateName;
    }
}
