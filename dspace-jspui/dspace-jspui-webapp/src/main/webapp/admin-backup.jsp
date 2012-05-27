<%-- 

    The contents of this file are subject to the license and copyright 
    detailed in the LICENSE and NOTICE files at the root of the source 
    tree and available online at 

    http://www.dspace.org/license/

--%> 
<%@page import="java.util.Set"%>
<%@page import="java.util.HashMap"%>
<%@page import="org.dspace.content.Item"%>
<%@page import="org.dspace.content.ItemIterator"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.io.IOException"%>
<%@page import="java.util.Map"%>
<%@page import="org.dspace.content.Community"%>
<%@page import="org.dspace.content.Collection"%>
<%-- 
    Document   : admin-backup
    Created on : Apr 22, 2012, 10:01:02 AM
    Author     : bitaites
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Community[] com = (Community[]) request.getAttribute("com");
    Map<Integer, Community[]> subComMap = 
            (Map<Integer, Community[]>) request.getAttribute("subComMap");
    Map<Integer, Collection[]> colMap = 
            (Map<Integer, Collection[]>) request.getAttribute("colMap");
    Map<Integer, ItemIterator> itemMap = 
            (Map<Integer, ItemIterator>) request.getAttribute("itemMap");
    Set<Integer> backupComDone = 
            (Set<Integer>) request.getAttribute("backupComDone");
    Set<Integer> backupColDone = 
            (Set<Integer>) request.getAttribute("backupColDone");
    Set<Integer> backupItemDone = 
            (Set<Integer>) request.getAttribute("backupItemDone");
    Set<Integer> cloudComExist = 
            (Set<Integer>) request.getAttribute("cloudComExist");
    Set<Integer> cloudColExist = 
            (Set<Integer>) request.getAttribute("cloudColExist");
    Set<Integer> cloudItemExist = 
            (Set<Integer>) request.getAttribute("cloudItemExist");
    Set<Integer> couldGetComFile = 
            (Set<Integer>) request.getAttribute("couldGetComFile");
    Set<Integer> couldGetColFile = 
            (Set<Integer>) request.getAttribute("couldGetColFile");
    Set<Integer> couldGetItemFile = 
            (Set<Integer>) request.getAttribute("couldGetItemFile");
    Set<Integer> couldDoRestoreCom = 
            (Set<Integer>) request.getAttribute("couldDoRestoreCom");
    Set<Integer> couldDoRestoreCol = 
            (Set<Integer>) request.getAttribute("couldDoRestoreCol");
    Set<Integer> couldDoRestoreItem = 
            (Set<Integer>) request.getAttribute("couldDoRestoreItem");
%>

<%!
    JspWriter out = null;
    HttpServletRequest request = null;
    
    void setContext(JspWriter out, HttpServletRequest request)
    { 
        this.out = out;
        this.request = request;
    }
    
    //funtion to show the communities and respective links
    void showCommunities(Community[] obj, 
            Map<Integer, Community[]> subObj,
            Map<Integer, Collection[]> collections,
            Map<Integer, ItemIterator> items,
            Set<Integer> backupComDone,
            Set<Integer> backupColDone,
            Set<Integer> backupItemDone,
            Set<Integer> cloudComExist,
            Set<Integer> cloudColExist,
            Set<Integer> cloudItemExist,
            Set<Integer> couldGetComFile, 
            Set<Integer> couldGetColFile, 
            Set<Integer> couldGetItemFile,
            Set<Integer> couldDoRestoreCom,
            Set<Integer> couldDoRestoreCol, 
            Set<Integer> couldDoRestoreItem) throws IOException, SQLException
    {
        out.println("<ul>");
        
        //for all the communities do:
        for(int i=0; i<obj.length; i++)
        {
            out.println("<li>");
            //link to community
            out.println("<a href=\"" + request.getContextPath() + "/handle/" + 
                    obj[i].getHandle() + "\">" + obj[i].getName() + "</a>");
            //link to backup
            if(backupComDone.contains(obj[i].getID()))
                out.println("backupDone");
            else
                out.println("  <a href=\"" + request.getContextPath() + "/backup/" + 
                    obj[i].getHandle() + "/this" + "\">" + "backup" + "</a>"); 
            //link to backup all
            out.println("  <a href=\"" + request.getContextPath() + "/backup/" + 
                    obj[i].getHandle() + "/all" + "\">" + "backupAll" + "</a>");
            //link to restore
            if(!couldDoRestoreCom.contains(obj[i].getID()))
                out.println("restoreNotAvailale");
            else
                out.println("  <a href=\"" + request.getContextPath() + "/restore/" + 
                    obj[i].getHandle() + "/this" + "\">" + "restore" + "</a>"); 
            //link to restore all
            out.println("  <a href=\"" + request.getContextPath() + "/restore/" + 
                    obj[i].getHandle() + "/all" + "\">" + "backupAll" + "</a>");
            //link to sendCloud
            if(cloudComExist.contains(obj[i].getID()))
                out.println("existCloud");
            else
                out.println("  <a href=\"" + request.getContextPath() + 
                        "/send-cloud/aws-s3/" + obj[i].getHandle() + "/this" + 
                        "\">" + "sendCloudAmazon" + "</a>");
            //link to sendCloud all
            out.println("  <a href=\"" + request.getContextPath() + 
                    "/send-cloud/aws-s3/" + obj[i].getHandle() + "/all" + 
                    "\">" + "sendAllCloudAmazon" + "</a>");
            //link to getCloud
            if(!couldGetComFile.contains(obj[i].getID()))
                out.println("getNotNecessary");
            else
                out.println("  <a href=\"" + request.getContextPath() + 
                        "/get-cloud/aws-s3/" + obj[i].getHandle() + "/this" + 
                        "\">" + "getCloudAmazon" + "</a>");
            //link to getCloud all
            out.println("  <a href=\"" + request.getContextPath() + 
                    "/get-cloud/aws-s3/" + obj[i].getHandle() + "/all" + 
                    "\">" + "getAllCloudAmazon" + "</a>");
            //if community contains sub-communities show them
            if(subObj.containsKey(obj[i].getID()))
            {
                Community[] newObj = subObj.get(obj[i].getID());
                showCommunities(newObj, subObj, collections, items, 
                        backupComDone, backupColDone, backupItemDone,
                        cloudComExist, cloudColExist, cloudItemExist,
                        couldGetComFile, couldGetColFile, couldGetItemFile,
                        couldDoRestoreCom, couldDoRestoreCol, couldDoRestoreItem);
            }
            //if community contains collections show them
            if(collections.containsKey(obj[i].getID()))
            {
                out.println("<br>");
                showCollections(collections.get(obj[i].getID()), items, 
                        backupColDone, backupItemDone,
                        cloudColExist, cloudItemExist,
                        couldGetColFile, couldGetItemFile,
                        couldDoRestoreCol, couldDoRestoreItem);
            }
            out.println("</li>");
        }
        out.println("</ul>");
    }
    
    //function to show the collections and respective links
    void showCollections(Collection[] col, 
            Map<Integer, ItemIterator> items,
            Set<Integer> backupColDone,
            Set<Integer> backupItemDone,
            Set<Integer> cloudColExist,
            Set<Integer> cloudItemExist,
            Set<Integer> couldGetColFile, 
            Set<Integer> couldGetItemFile,
            Set<Integer> couldDoRestoreCol, 
            Set<Integer> couldDoRestoreItem) throws IOException, SQLException
    {
        out.println("<ul>");
        //for all collections do:
        for(int i=0; i<col.length; i++)
        {
            out.println("<li>");
            //link to collection
            out.println("<a href=\"" + request.getContextPath() + "/handle/" + 
                    col[i].getHandle() + "\">" + col[i].getName() + "</a>");
            //link to backup
            if(backupColDone.contains(col[i].getID()))
                out.println("backupDone");
            else
                out.println("  <a href=\"" + request.getContextPath() + "/backup/" + 
                    col[i].getHandle() + "/this" + "\">" + "backup" + "</a>");
            //link to backup all
            out.println("  <a href=\"" + request.getContextPath() + "/backup/" + 
                    col[i].getHandle() + "/all" + "\">" + "backupAll" + "</a>");
            //link to restore
            if(!couldDoRestoreCol.contains(col[i].getID()))
                out.println("restoreNotAvailale");
            else
                out.println("  <a href=\"" + request.getContextPath() + "/restore/" + 
                    col[i].getHandle() + "/this" + "\">" + "restore" + "</a>"); 
            //link to restore all
            out.println("  <a href=\"" + request.getContextPath() + "/restore/" + 
                    col[i].getHandle() + "/all" + "\">" + "backupAll" + "</a>");
            //link to sendCloud
            if(cloudColExist.contains(col[i].getID()))
                out.println("existCloud");
            else
                out.println("  <a href=\"" + request.getContextPath() + 
                        "/send-cloud/aws-s3/" + col[i].getHandle() + "/this" + 
                        "\">" + "sendCloudAmazon" + "</a>");
            //link to sendCloud all
            out.println("  <a href=\"" + request.getContextPath() + 
                    "/send-cloud/aws-s3/" + col[i].getHandle() + "/all" + 
                    "\">" + "sendAllCloudAmazon" + "</a>");
            //link to getCloud
            if(!couldGetColFile.contains(col[i].getID()))
                out.println("getNotNecessary");
            else
                out.println("  <a href=\"" + request.getContextPath() + 
                        "/get-cloud/aws-s3/" + col[i].getHandle() + "/this" + 
                        "\">" + "getCloudAmazon" + "</a>");
            //link to getCloud all
            out.println("  <a href=\"" + request.getContextPath() + 
                    "/get-cloud/aws-s3/" + col[i].getHandle() + "/all" + 
                    "\">" + "getAllCloudAmazon" + "</a>");
            //show items if collections contais
            if(items.containsKey(col[i].getID()))
                showItems(items.get(col[i].getID()), backupItemDone, 
                        cloudItemExist, couldGetItemFile, couldDoRestoreItem);
            out.println("</li>");
        }
        out.println("</ul>");
    }
    
    //function to show the items and respective links
    void showItems(ItemIterator obj, 
            Set<Integer> backupItemDone,
            Set<Integer> cloudItemExist,
            Set<Integer> couldGetItemFile,
            Set<Integer> couldDoRestoreItem) throws IOException, SQLException
    {
        out.println("<ul>");
        while(obj.hasNext())
        {
            out.println("<li>");
            Item newObj = obj.next();
            //link to item
            out.println("<a href=\"" + request.getContextPath() + "/handle/" + 
                    newObj.getHandle() + "\">" + newObj.getName() + "</a>");
            //link to backup
            if(backupItemDone.contains(newObj.getID()))
                out.println("backupDone");
            else
                out.println("  <a href=\"" + request.getContextPath() + "/backup/" + 
                    newObj.getHandle() + "/this" + "\">" + "backup" + "</a>");
            //link to restore
            if(!couldDoRestoreItem.contains(newObj.getID()))
                out.println("restoreNotAvailale");
            else
                out.println("  <a href=\"" + request.getContextPath() + "/restore/" + 
                    newObj.getHandle() + "/this" + "\">" + "restore" + "</a>"); 
            //link to sendCloud
            if(cloudItemExist.contains(newObj.getID()))
                out.println("existCloud");
            else
                out.println("  <a href=\"" + request.getContextPath() + 
                        "/send-cloud/aws-s3/" + newObj.getHandle() + "\">" + 
                        "sendCloudAmazon" + "</a>");
            //link to getCloud
            if(!couldGetItemFile.contains(newObj.getID()))
                out.println("getNotNecessary");
            else
                out.println("  <a href=\"" + request.getContextPath() + 
                        "/get-cloud/aws-s3/" + newObj.getHandle() + "\">" + 
                        "getCloudAmazon" + "</a>");
            out.println("</li>");
        }
        out.println("</ul>");
    }
%>

<dspace:layout titlekey="jsp.admin-backup.title">

    <%  
        setContext(out, request);
        
        showCommunities(com, subComMap, colMap, itemMap, 
                backupComDone, backupColDone, backupItemDone,
                cloudComExist, cloudColExist, cloudItemExist,
                couldGetComFile, couldGetColFile, couldGetItemFile,
                couldDoRestoreCom, couldDoRestoreCol, couldDoRestoreItem);
    %>           
        
    <br> <br>
    
</dspace:layout>