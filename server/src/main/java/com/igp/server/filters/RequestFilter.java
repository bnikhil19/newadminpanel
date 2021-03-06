package com.igp.server.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igp.handles.vendorpanel.response.ResponseModel;
import com.igp.handles.vendorpanel.response.Status;
import com.igp.config.Environment;
import com.igp.config.ResponseProperties;
import com.igp.permit.endpoints.Permission;
import com.igp.server.utils.RequestHeader;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestFilter implements Filter {

    public void init(FilterConfig config) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        // For HTTP OPTIONS verb/method reply with ACCEPTED status code -- per CORS handshake
        if(httpServletRequest.getMethod().equals("OPTIONS")){
            HttpServletResponse httpServletResponse=(HttpServletResponse)response;
            httpServletResponse.addHeader("Access-Control-Allow-Origin", "*");
            httpServletResponse.addHeader("Access-Control-Allow-Methods","GET, OPTIONS, HEAD, PUT, POST, DELETE");
            httpServletResponse.addHeader("Access-Control-Allow-Headers", Environment.getAllowedHeadersForPreFlightCall());
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        RequestHeader requestHeader = new RequestHeader(httpServletRequest);
        if (requestHeader.verifyAuthorizationKey(httpServletRequest)) {
            Map<String, Boolean> regionLang = requestHeader.regionLang(httpServletRequest);
            if (Boolean.FALSE.equals(regionLang.get("region")) || Boolean.FALSE.equals(regionLang.get("lang"))) {
                regionLangNotSupported((HttpServletResponse) response);
            } else {
                // check all the permissions for a user
                System.out.println(" before dofilter request header :"+requestHeader.toString());

                Permission permission = new Permission();
                boolean result = permission.checkPermission(httpServletRequest);

                if(result==true) {
                    // user is permitted
                    chain.doFilter(requestHeader, response);
                }else{
                    // user has no permissions to access the requested endpoints
                    permissionNotGranted((HttpServletResponse) response);
                    System.out.println("modify request header, cant process the request.");
                }
                System.out.println(" after dofilter");
            }
        } else {
            authKeyNotSupported((HttpServletResponse) response);
        }
    }

    private void permissionNotGranted(HttpServletResponse response) throws IOException {
        Status status = Status.Error;
        Response.Status httpStatus = Response.Status.UNAUTHORIZED;
        String message = "This user is not permitted to use this.";
        Map<String,String> data1 = new HashMap<>();
        data1.put("Error","You are not Permitted to access this.");
        ResponseModel responseModel = new ResponseModel.ResponseBuilder(status, httpStatus)
            .message(message)
            .data(data1)
            .build();
        ObjectMapper objectMapper = new ObjectMapper();
        String data = objectMapper.writeValueAsString(responseModel);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getOutputStream().print(data);
    }

    private void authKeyNotSupported(HttpServletResponse response) throws IOException {
        Status status = Status.Error;
        Response.Status httpStatus = Response.Status.UNAUTHORIZED;
        String message = ResponseProperties.getAuthKeyNotSupported();
        ResponseModel responseModel = new ResponseModel.ResponseBuilder(status, httpStatus)
            .message(message)
            .build();
        ObjectMapper objectMapper = new ObjectMapper();
        String data = objectMapper.writeValueAsString(responseModel);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getOutputStream().print(data);
    }

    private void regionLangNotSupported(HttpServletResponse response) throws IOException {
        Status status = Status.Error;
        Response.Status httpStatus = Response.Status.BAD_REQUEST;
        String message = ResponseProperties.getRegionLangNotSupported();
        ResponseModel responseModel = new ResponseModel.ResponseBuilder(status, httpStatus)
            .message(message)
            .build();
        ObjectMapper objectMapper = new ObjectMapper();
        String data = objectMapper.writeValueAsString(responseModel);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getOutputStream().print(data);
    }

    public void destroy() {
    }

}
