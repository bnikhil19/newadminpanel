package com.igp.handles.admin.mappers.Order;

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

import static java.lang.Math.abs;

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
                }
            }

            List<OrdersProducts> orderProductList = orderUtilVendorPanel.getOrderProductsByStatusDate("1",fkassociateId,status,date,
                section,  isfuture,ordersProductExtraInfoMap,true,slaClause);
            orders=orderMapperVendorPanel.prepareOrders(orderAction,orderProductList,ordersProductExtraInfoMap,"",true);

        }catch (Exception exception){
            logger.error("error while getOrderByStatusDate",exception);
        }

        return orders;
    }
    public int assignReassignOrder(String action,int orderId,String orderProductIdString,int vendorId,String allOrderProductIdList,
        List<Order> orderList,HandleServiceResponse handleServiceResponse){
        int result=0;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();

        String restOrderProductIdList="";
        boolean orderIsProcessedOrNot=false;
        try{
            String[] orderProductId=orderProductIdString.split(",");
            restOrderProductIdList=findIntersectionOfTwoCommaSeparatedStrings(orderProductIdString,allOrderProductIdList);

            for(int i=0;i<orderProductId.length;i++){
                Order order=orderUtil.getOrderRelatedInfo(orderId,Integer.parseInt(orderProductId[i]));
                OrdersProducts ordersProducts=order.getOrderProducts().get(0);

                logger.debug("step-2 assignReassignOrder with orderProductId "+orderProductId+" and restOrderProductIdList "+restOrderProductIdList);

                if(action.equalsIgnoreCase("assign")){
                    result=orderUtil.assignOrderToVendor(orderId,Integer.parseInt(orderProductId[i]),vendorId,order);
                    if(result!=1){
                        if(restOrderProductIdList.equals("")){
                            restOrderProductIdList+=orderProductId[i]+",";
                        }else {
                            restOrderProductIdList+=","+orderProductId[i]+",";
                        }
                    }else if(result==2||result==3){
                        return result;
                    }
                }else if(action.equalsIgnoreCase("reassign")) {
                    result=orderUtil.reassignOrderToVendor(orderId,Integer.parseInt(orderProductId[i]),vendorId,order);

                    if(ordersProducts.getOrdersProductStatus().equals("Processed")
                        &&!ordersProducts.getFkAssociateId().equals("72")){
                        orderIsProcessedOrNot=true;
                    }
                    if(result!=1){
                        if(restOrderProductIdList.equals("")){
                            restOrderProductIdList+=orderProductId[i]+",";
                        }else {
                            restOrderProductIdList+=","+orderProductId[i]+",";
                        }
                    }else if(result==2||result==3){
                        return result;
                    }
                }
            }
            if(!restOrderProductIdList.equals("") && restOrderProductIdList.charAt(restOrderProductIdList.length()-1)==','){
                restOrderProductIdList=restOrderProductIdList.substring(0,restOrderProductIdList.length()-1);
            }
            String orderProductIdsWhichAreActuallyAssigned=findIntersectionOfTwoCommaSeparatedStrings(restOrderProductIdList,orderProductIdString);

            if(action.equalsIgnoreCase("assign")){
                if(!restOrderProductIdList.equals("")){
                    orderList=getOrder(orderId,restOrderProductIdList);
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
                    }
                }
            }
            //mailService will be integrated here with orderProductIdsWhichAreActuallyAssigned
            handleServiceResponse.setResult(orderList);

        }catch (Exception exception){
            logger.error("error while assignReassignOrder",exception);
        }

        return result;
    }
    public boolean orderPriceChanges(int orderId,int orderProductId,int componentId,Double componentPrice,Double shippingCharge){
        boolean result=false;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        Double vendorPrice=null;
        int productId=0;
        try {
            productId=orderUtil.getProductId(orderProductId);
            OrderComponent orderComponent=orderUtil.getOrderComponent(orderId,productId,componentId);

            if(componentPrice!=null && orderComponent != null){
                orderUtil.updateComponentPriceOrderLevel(orderId,productId,componentId,componentPrice);
                vendorPrice = abs(Double.valueOf(orderComponent.getQuantity())*Double.valueOf(orderComponent.getComponentPrice())
                    - Double.valueOf(orderComponent.getQuantity())*componentPrice ) ;
            }

            result=orderUtil.updateVendorAssignPrice(orderId,productId,vendorPrice,shippingCharge,orderProductId);

        }catch (Exception exception){
            logger.error("error while orderComponentPriceChange",exception);
        }
        return result;
    }
    public boolean deliveryDetailChanges(int orderId,int orderProductId,String deliveryDate,String deliveryTime,int deliveryType,
        HandleServiceResponse handleServiceResponse,String orderProductIdList){
        boolean result=false;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        int productId=0;
        List<Order> orderList=new ArrayList<>();
        String restOrderProductIdList="";
        try{
            restOrderProductIdList=findIntersectionOfTwoCommaSeparatedStrings(String.valueOf(orderProductId),orderProductIdList);
            productId=orderUtil.getProductId(orderProductId);
            result=orderUtil.updateDeliveryDetails(orderId,orderProductId,productId,deliveryDate,deliveryTime,deliveryType);
            if(result){
                if(deliveryDate!=null){
                    if(!restOrderProductIdList.equals("")){
                        orderList=getOrder(orderId,restOrderProductIdList);
                    }
                }else{
                    if(!restOrderProductIdList.equals("")){
                        orderList=getOrder(orderId,String.valueOf(orderProductId));
                        orderList=mergeOrderList(orderList,getOrder(orderId,restOrderProductIdList));
                    }else {
                        orderList=getOrder(orderId,String.valueOf(orderProductId));
                    }
                }
                handleServiceResponse.setResult(orderList);
            }
        }catch (Exception exception){
            logger.error("error while deliveryDetailChanges",exception);
        }
        return result;
    }
    public String getOrderLog(int orderId){
        String logs="";
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        try {
            logs=orderUtil.getOrderLog(orderId);
        }catch (Exception exception){
            logger.error("error while getting OrderLog",exception);

        }
        return logs;
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
    public boolean cancelOrder(int orderId,int orderProductId,String comment,HandleServiceResponse handleServiceResponse,String orderProductIdList){
        boolean result=false;
        com.igp.handles.admin.utils.Order.OrderUtil orderUtil=new com.igp.handles.admin.utils.Order.OrderUtil();
        String restOrderProductIdList="";
        List<Order> orderList=new ArrayList<>();
        try {
            restOrderProductIdList=findIntersectionOfTwoCommaSeparatedStrings(String.valueOf(orderProductId),orderProductIdList);
            if(!restOrderProductIdList.equals("")){
                orderList=getOrder(orderId,restOrderProductIdList);
            }
            handleServiceResponse.setResult(orderList);
            result=orderUtil.cancelOrder(orderId,orderProductId,comment);
        }catch (Exception exception){
            logger.error("error while cancelling ",exception);
        }
        return result;
    }
    public List<Order> mergeOrderList(List<Order> orderList,List<Order> orderList1){
        return ListUtils.union(orderList,orderList1);
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
}
