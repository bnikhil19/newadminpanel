package com.igp.handles.admin.models.Vendor;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by shanky on 26/1/18.
 */
public class VendorInfoModel {
    @JsonProperty("fkAssociateId")
    private int vendorId;

    @JsonProperty("associateName")
    private String associateName;

    public int getVendorId()
    {
        return vendorId;
    }

    public void setVendorId(int vendorId)
    {
        this.vendorId = vendorId;
    }

    public String getAssociateName()
    {
        return associateName;
    }

    public void setAssociateName(String associateName)
    {
        this.associateName = associateName;
    }
}
