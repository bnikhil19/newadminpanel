package com.igp.handles.admin.mappers.Order;

import com.igp.handles.admin.models.Order.OrderLogModel;
import com.igp.handles.admin.utils.MailUtil.MailUtil;
import com.igp.handles.vendorpanel.models.Order.Order;
import com.igp.handles.vendorpanel.models.Order.OrderComponent;
import com.igp.handles.vendorpanel.models.Order.OrderProductExtraInfo;
import com.igp.handles.vendorpanel.models.Order.OrdersProducts;
import com.igp.handles.vendorpanel.response.HandleServiceResponse;
import com.igp.handles.vendorpanel.utils.Order.OrderUtil;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by shanky on 22/1/18.
 */
public class OrderMapper {
    private static final Logger logger = LoggerFactory.getLogger(OrderMapper.class);
    public List<Order> getOrderByStatusDate(String category ,String subCategory, Date date,
        String orderAction, String section, boolean isfuture){

        List<Order> orders=new ArrayList<>();
        OrderUtil orderUtilVendorPanel=new OrderUtil();
        com.igp.handles.vendorpanel.mappers.Order.OrderMapper orderMapperVendorPanel=new com.igp.handles.vendorpanel.mappers.Order.OrderMapper();
        String status="",slaClause=null;
        int fkassociateId=-1;
        int deliveryAttemptFlag=0;
        Map<Integer, OrderProductExtraInfo> ordersProductExtraInfoMap = new HashMap<>();
        try{

            if(category.equals("unAssigned")){
                if(subCategory.equals("notAlloted")){
                    status="Processed";
                    fkassociateId=72;
                }else if(subCategory.equals("processing")) {
                    status="Processing";
                    fkassociateId=72;
                }
            }else if(category.equals("notConfirmed")){
                if(subCategory.equals("pending")){
                    status="Processed";
                    slaClause="(100,101,102)";
                }else if(subCategory.equals("total")){
                    status="Processed";
                }
            }else if(category.equals("notShipped")){
                if(subCategory.equals("pending")){
                    status="Confirmed";
                    slaClause="(100,201,202,203,204)";
                }else if(subCategory.equals("total")){
                    status="Confirmed";
                }
            }else if(category.equals("notDelivered")){
                if(subCategory.equals("pending")){
                    status="OutForDelivery";
                    slaClause="(100,401,402,403,404)";
                }else if(subCategory.equals("total")){
                    status="OutForDelivery";
                }else if(subCategory.equals("attemptedDelivery")){
                    status="Shipped";
                    deliveryAttemptFlag=1;
                }
            }

            List<OrdersProducts> orderProductList = orderUtilVendorPanel.getOrderProductsByStatusDate("1",fkassociateId,status,date,
                section,  isfuture,ordersProductExtraInfoMap,true,slaClause,deliveryAttemptFlag);
            orders=orderMapperVendorPanel.prepareOrders(orderAction,orderProductList,ordersProductExtraInfoMap,"",true);

        }catch (Exception exception){
            logger.error("error while getOrderByStatusDate",exception);
        }

        return orders;
    }
    public Set<String> assignReassignOrder(String action,int orderId,String orderProductIdString,int vendorId,String allOrderProductIdList,
        List<Order> orderList,HandleServiceResponse handleServiceResponse,String ipAddress,String userAgent){
        int result=0;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        Set<String> responseMap=new HashSet<>();
        String restOrderProductIdList="",mailerAction;
        boolean orderIsProcessedOrNot=false;
        MailUtil mailUtil=new MailUtil();
        try{
            String[] orderProductId=orderProductIdString.split(",");
            restOrderProductIdList=findIntersectionOfTwoCommaSeparatedStrings(orderProductIdString,allOrderProductIdList);

            for(int i=0;i<orderProductId.length;i++){
                if(orderProductId[i]!=null && !orderProductId[i].equals("")){
                    Order order=orderUtil.getOrderRelatedInfo(orderId,Integer.parseInt(orderProductId[i]));
                    OrdersProducts ordersProducts=order.getOrderProducts().get(0);

                    logger.debug("step-2 assignReassignOrder with orderProductId "+orderProductId+" and restOrderProductIdList "+restOrderProductIdList);

                    if(action.equalsIgnoreCase("assign")){
                        result=orderUtil.assignOrderToVendor(orderId,Integer.parseInt(orderProductId[i]),vendorId,order,action,ipAddress,userAgent);
                        if(result!=1){
                            if(restOrderProductIdList.equals("")){
                                restOrderProductIdList+=orderProductId[i];
                            }else {
                                restOrderProductIdList+=","+orderProductId[i];
                            }
                        }
                    }else if(action.equalsIgnoreCase("reassign")) {
                        result=orderUtil.reassignOrderToVendor(orderId,Integer.parseInt(orderProductId[i]),vendorId,order,action,ipAddress,userAgent);

                        if(ordersProducts.getOrdersProductStatus().equals("Processed")
                            &&!ordersProducts.getFkAssociateId().equals("72")){
                            orderIsProcessedOrNot=true;
                        }
                        if(result!=1){
                            if(restOrderProductIdList.equals("")){
                                restOrderProductIdList+=orderProductId[i]+",";
                            }else {
                                restOrderProductIdList+=","+orderProductId[i];
                            }
                        }
                    }
                }
            }
            if(!restOrderProductIdList.equals("") && restOrderProductIdList.charAt(restOrderProductIdList.length()-1)==','){
                restOrderProductIdList=restOrderProductIdList.substring(0,restOrderProductIdList.length()-1);
            }
            String orderProductIdsWhichAreActuallyAssigned=findIntersectionOfTwoCommaSeparatedStrings(restOrderProductIdList,orderProductIdString);
            if(orderProductIdsWhichAreActuallyAssigned!=null && !orderProductIdsWhichAreActuallyAssigned.equals("")){
                fillAssignReassignResponseMap(orderId,orderProductIdsWhichAreActuallyAssigned,responseMap,0);
            }
            if(action.equalsIgnoreCase("assign")){
                if(!restOrderProductIdList.equals("")){
                    orderList=getOrder(orderId,restOrderProductIdList);
                    fillAssignReassignResponseMap(orderId,restOrderProductIdList,responseMap,1);
                }
            }else if(action.equalsIgnoreCase("reassign")) {
                if(orderIsProcessedOrNot){
                    if(!restOrderProductIdList.equals("")){

                        //this is because if something happend while assigning and reassigning orderProduct then it will be in the other list
                        orderList=getOrder(orderId,orderProductIdsWhichAreActuallyAssigned);
                        orderList = mergeOrderList(orderList, getOrder(orderId,restOrderProductIdList));
                    }else {
                        orderList=getOrder(orderId,orderProductIdString);
                    }
                }else {
                    if(!restOrderProductIdList.equals("")){
                        orderList=getOrder(orderId,restOrderProductIdList);
                        fillAssignReassignResponseMap(orderId,restOrderProductIdList,responseMap,1);
                    }
                }
            }
//            mailService will be integrated here with orderProductIdsWhichAreActuallyAssigned
//            mailerAction="assignorder&orderid="+orderId+"&orderproductids="+orderProductIdsWhichAreActuallyAssigned+"&associd="+vendorId;
//            if(mailUtil.sendGenericMail(mailerAction,"","","",false)){
//                logger.debug("Mail successfully sent for assign/reassign of orderId "+orderId+" with orderProductId "+orderProductIdsWhichAreActuallyAssigned);
//            }
            handleServiceResponse.setResult(orderList);

        }catch (Exception exception){
            logger.error("error while assignReassignOrder",exception);
        }

        return responseMap;
    }
    public boolean orderPriceChanges(int orderId,int orderProductId,Integer componentId,Double componentPrice,Double shippingCharge,String ipAddress,String userAgent){
        boolean result=false;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        Double vendorPrice=null;
        int productId=0;
        String mailerAction;
        MailUtil mailUtil=new MailUtil();
        try {
            List<OrdersProducts> ordersProducts=orderUtil.getOrderProductList(String.valueOf(orderProductId));
            productId=ordersProducts.get(0).getProductId();
            String fkAssociateId=ordersProducts.get(0).getFkAssociateId();
            if(componentId!=null){
                OrderComponent orderComponent=orderUtil.getOrderComponent(orderId,productId,componentId.intValue());
                if(componentPrice!=null && orderComponent != null){
                    orderUtil.updateComponentPriceOrderLevel(orderId,productId,componentId.intValue(),componentPrice);
                    vendorPrice = Double.valueOf(orderComponent.getQuantity())*componentPrice ;
                }
            }
            if(fkAssociateId!=null && !fkAssociateId.equals("")){
                result=orderUtil.updateVendorAssignPrice(orderId,productId,vendorPrice,shippingCharge,orderProductId,Integer.parseInt(fkAssociateId),ipAddress,userAgent);
            }

            if(result){
                mailerAction="orderpricechange&orderid="+orderId+"&orderproductids="+orderProductId+"&associd="+ordersProducts.get(0).getFkAssociateId();
                if(mailUtil.sendGenericMail(mailerAction,"","","",false)){
                    logger.debug("Mail successfully sent for Price changes of orderId "+orderId+" with orderProductId "+orderProductId);
                }
            }

        }catch (Exception exception){
            logger.error("error while orderComponentPriceChange",exception);
        }
        return result;
    }
    public boolean deliveryDetailChanges(int orderId,int orderProductId,String deliveryDate,String deliveryTime,int deliveryType,
        HandleServiceResponse handleServiceResponse,String orderProductIdList,String ipAddress,String userAgent){
        boolean result=false;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        int productId=0;
        List<Order> orderList=new ArrayList<>();
        String restOrderProductIdList="";
        String mailerAction;
        MailUtil mailUtil=new MailUtil();
        try{
            restOrderProductIdList=findIntersectionOfTwoCommaSeparatedStrings(String.valueOf(orderProductId),orderProductIdList);
            List<OrdersProducts> ordersProducts=orderUtil.getOrderProductList(String.valueOf(orderProductId));
            productId=ordersProducts.get(0).getProductId();
            String fkAssociateId=ordersProducts.get(0).getFkAssociateId();
            if(fkAssociateId!=null && !fkAssociateId.equals("")){
                result=orderUtil.updateDeliveryDetails(orderId,orderProductId,productId,deliveryDate,deliveryTime,deliveryType,Integer.parseInt(fkAssociateId),ipAddress,userAgent);
            }
            if(result){
                if(deliveryDate!=null){
                    if(!restOrderProductIdList.equals("")){
                        orderList=getOrder(orderId,restOrderProductIdList);
                    }
                    mailerAction="orderdeliverychange&orderid="+orderId+"&orderproductids="+orderProductId+"&associd="+ordersProducts.get(0).getFkAssociateId();
                    if(mailUtil.sendGenericMail(mailerAction,"","","",false)){
                        logger.debug("Mail successfully sent for Delivery Detail changes of  orderId "+orderId+" with orderProductId "+orderProductId);
                    }
                }else{
                    orderList=getOrder(orderId,orderProductIdList);
                }
                handleServiceResponse.setResult(orderList);
            }
        }catch (Exception exception){
            logger.error("error while deliveryDetailChanges",exception);
        }
        return result;
    }
    public List<OrderLogModel> getOrderLog(int orderId,String type,String fkAssociateId){
        List<OrderLogModel> orderLogModelList=new ArrayList<>();
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        try {
            orderLogModelList =orderUtil.getOrderLog(orderId,type,fkAssociateId);
        }catch (Exception exception){
            logger.error("error while getting OrderLog",exception);

        }
        return orderLogModelList;
    }
    public List<Order> getOrder(int orderId,String orderProductIdList){
        List<Order> orders = new ArrayList<>();
        OrderUtil orderUtil=new OrderUtil();
        com.igp.handles.vendorpanel.mappers.Order.OrderMapper orderMapper=new com.igp.handles.vendorpanel.mappers.Order.OrderMapper();

        Map<Integer, OrderProductExtraInfo> ordersProductExtraInfoMap = new HashMap<>();
        try{
            List<OrdersProducts> orderProductList = orderUtil.getOrderProducts("1", orderId, "",ordersProductExtraInfoMap,orderProductIdList,true);
            orders=orderMapper.prepareOrders("all",orderProductList,ordersProductExtraInfoMap,"",true);

        }
        catch (Exception e){
            logger.error("Exception while generating orders",e);
        }
        return orders;
    }
    public boolean cancelOrder(int orderId,String orderProductIdString,String comment,HandleServiceResponse handleServiceResponse,String allOrderProductIdList,String ipAddress,String userAgent){
        boolean result=false;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        String restOrderProductIdList="";
        List<Order> orderList=new ArrayList<>();
        String mailerAction;
        MailUtil mailUtil=new MailUtil();
        try {
            restOrderProductIdList=findIntersectionOfTwoCommaSeparatedStrings(orderProductIdString,allOrderProductIdList);


            List<OrdersProducts> ordersProducts=orderUtil.getOrderProductList(orderProductIdString);
            if(!restOrderProductIdList.equals("")){
                orderList=getOrder(orderId,restOrderProductIdList);
            }
            result=orderUtil.cancelOrder(orderId,orderProductIdString,comment,ipAddress,userAgent);
            if(result){
                mailerAction="cancelorder&orderid="+orderId+"&orderproductids="+orderProductIdString+"&associd="+ordersProducts.get(0).getFkAssociateId();

                if(mailUtil.sendGenericMail(mailerAction,"","","",false)){
                    logger.debug("Mail successfully sent for cancelling orderId "+orderId+" with orderProductId "+orderProductIdString);
                }
                handleServiceResponse.setResult(orderList);
            }
        }catch (Exception exception){
            logger.error("error while cancelling ",exception);
        }
        return result;
    }
    public List<Order> mergeOrderList(List<Order> orderList,List<Order> orderList1){
        return ListUtils.union(orderList,orderList1);
    }

    public boolean approveDeliveryAttempt(int orderId,String orderProductIdList,String ipAddress,String userAgent){
        boolean result=false;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        try{
            result=orderUtil.updateOrderProductForApproveAttemptedDeliveryOrder(orderId,orderProductIdList,ipAddress,userAgent);
        }catch(Exception exception){
            logger.error("error while approveDeliveryAttempt ",exception);
        }

        return result;
    }
    public boolean addVendorInstruction(int orderId,String orderProductIdString,int fkAssociateId,String instruction,String ipAddress,String userAgent){
        boolean result=false;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        try{
            List<OrdersProducts> orderProductList=orderUtil.getOrderProductList(orderProductIdString);
            for(OrdersProducts ordersProducts:orderProductList){
                if(ordersProducts.getFkAssociateId()!=null && !ordersProducts.getFkAssociateId().equals("")){
                    result=orderUtil.insertIntoHandelOrderHistory(orderId,ordersProducts.getProductId(),Integer.parseInt(ordersProducts.getFkAssociateId()),instruction,ipAddress,userAgent,"instruction","from_igp");
                    //here fk_associate_id is for whom this instrution is supposed to go i.e vendorID  i.e for fetching the instruction for certain vendor from IGP we just have to provide the vendorID for that vendor also
                }
            }
        }catch(Exception exception){
            logger.error("error while adding VendorInstruction ",exception);
        }
        return result;
    }

    public Set<String> bulkAssign(Map<String,Map<String,String>> bulkAssignOrderIdMap,String ipAddress,String userAgent,HandleServiceResponse handleServiceResponse){
        Integer orderId=null;
        Integer vendorId=null;
        String orderProductId=null,commaSeparatedOrderProductId=null;
        Map<Integer,Map<Integer,String>> vendorIdToOrderIdToOrderProductIdMap=new HashMap<>();
        Set<String> responseMap=new HashSet<>();
        try{
            for(Map.Entry<String,Map<String,String>> entry:bulkAssignOrderIdMap.entrySet()){
                orderId=Integer.parseInt(entry.getKey());
                Map<String,String> orderProdIdToVendorIdMap=entry.getValue();
                for(Map.Entry<String,String> entry1:orderProdIdToVendorIdMap.entrySet()){
                    vendorId=Integer.parseInt(entry1.getValue());
                    orderProductId=entry1.getKey();

                    if(!vendorIdToOrderIdToOrderProductIdMap.containsKey(vendorId)){
                        Map<Integer,String> orderIdToOrderProductIdMap=new HashMap<>();
                        orderIdToOrderProductIdMap.put(orderId,orderProductId);
                        vendorIdToOrderIdToOrderProductIdMap.put(vendorId,orderIdToOrderProductIdMap);
                    }else{
                        if(!vendorIdToOrderIdToOrderProductIdMap.get(vendorId).containsKey(orderId)){
                            vendorIdToOrderIdToOrderProductIdMap.get(vendorId).put(orderId,orderProductId);
                        }else{
                            commaSeparatedOrderProductId=vendorIdToOrderIdToOrderProductIdMap.get(vendorId).get(orderId)+","+orderProductId;
                            vendorIdToOrderIdToOrderProductIdMap.get(vendorId).put(orderId,commaSeparatedOrderProductId);
                        }
                    }
                }
            }

            for(Map.Entry<Integer,Map<Integer,String>> entry:vendorIdToOrderIdToOrderProductIdMap.entrySet()){
                vendorId=entry.getKey();
                for(Map.Entry<Integer,String> entry1:entry.getValue().entrySet()){
                    orderId=entry1.getKey();
                    commaSeparatedOrderProductId=entry1.getValue();
                    List<com.igp.handles.vendorpanel.models.Order.Order> orderList=new ArrayList<>();
                    responseMap=assignReassignOrder("assign",orderId,commaSeparatedOrderProductId,vendorId
                        ,commaSeparatedOrderProductId,orderList,handleServiceResponse,ipAddress,userAgent);
                }
            }
        }catch (Exception exception){
            logger.error("error while bulkAssign ",exception);
        }
        return responseMap;
    }

    public String findIntersectionOfTwoCommaSeparatedStrings(String one,String two){
        // ( A U B ) == B   , ( A ∩ B ) == A  , !( A ∩ B )  part of B which is not in A .....   consider B as a big circle and A is reside withIn B
        String interSection="";
        String[] allElements=two.split(",");
        for(int i=0;i<allElements.length;i++){
            if(!one.contains(allElements[i])){
                interSection+=allElements[i]+",";
            }
        }
        if(!interSection.equals("")){
            interSection=interSection.substring(0,interSection.length()-1);
        }
        return interSection;
    }
    public String[] findCategoryAndSubCategory(String orderProductStatus,int fkAssociateId,int deliveryStatus,int slacode){
        String[] catSubCatArray=new String[2];
        catSubCatArray[0]="";
        catSubCatArray[1]="";
        switch (orderProductStatus){
            case "Processing":
                catSubCatArray[0]="unAssigned";
                catSubCatArray[1]="processing";
                break;
            case "Processed":
                if(fkAssociateId==72){
                    catSubCatArray[0]="unAssigned";
                    catSubCatArray[1]="notAlloted";
                }else {
                    if(OrderUtil.isHighAlertActionRequired(slacode)){
                        catSubCatArray[0]="notConfirmed";
                        catSubCatArray[1]="pending";
                    }else {
                        catSubCatArray[0]="notConfirmed";
                        catSubCatArray[1]="total";
                    }
                }
                break;
            case "Confirmed":
                if(OrderUtil.isHighAlertActionRequired(slacode)){
                    catSubCatArray[0]="notShipped";
                    catSubCatArray[1]="pending";
                }else {
                    catSubCatArray[0]="notShipped";
                    catSubCatArray[1]="total";
                }
                break;
            case "Shipped":
                if(deliveryStatus==0){
                    if(OrderUtil.isHighAlertActionRequired(slacode)){
                        catSubCatArray[0]="notDelivered";
                        catSubCatArray[1]="pending";
                    }else {
                        catSubCatArray[0]="notDelivered";
                        catSubCatArray[1]="total";
                    }
                }
                break;
        }
        return catSubCatArray;
    }
    public void fillAssignReassignResponseMap(int orderId,String orderProductIdString,Set<String> responseMap,int flag){
        //flag=0 for Successful Assign and flag=1 for Could not ASSIGN
        String[] orderProductIdArray=orderProductIdString.split(",");
        for(int i=0;i<orderProductIdArray.length;i++){
            if(flag==0){
                responseMap.add("Order Id "+orderId+" successfully assigned with orderProduct Id "+orderProductIdArray[i]);
            }else {
                responseMap.add("Order Id "+orderId+" could not assign with orderProduct Id "+orderProductIdArray[i]);
            }
        }


    }
}
