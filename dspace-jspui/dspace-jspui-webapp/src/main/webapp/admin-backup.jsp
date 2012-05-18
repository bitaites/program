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
    String frase = (String) request.getAttribute("hello");
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
            Set<Integer> backupColDone) throws IOException, SQLException
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
            //link to sendCloud
            out.println("  <a href=\"" + request.getContextPath() + "/send-cloud/aws-s3/" + 
                    obj[i].getHandle() + "/this" + "\">" + "sendCloudAmazon" + "</a>");
            //link to sendCloud all
            out.println("  <a href=\"" + request.getContextPath() + "/send-cloud/aws-s3/" + 
                    obj[i].getHandle() + "/all" + "\">" + "sendAllCloudAmazon" + "</a>");
            //if community contains sub-communities show them
            if(subObj.containsKey(obj[i].getID()))
            {
                Community[] newObj = subObj.get(obj[i].getID());
                showCommunities(newObj, subObj, collections, items, 
                        backupComDone, backupColDone);
            }
            //if community contains collections show them
            if(collections.containsKey(obj[i].getID()))
            {
                out.println("<br>");
                showCollections(collections.get(obj[i].getID()), items, backupColDone);
            }
            out.println("</li>");
        }
        out.println("</ul>");
    }
    
    //function to show the collections and respective links
    void showCollections(Collection[] col, 
            Map<Integer, ItemIterator> items,
            Set<Integer> backupColDone) throws IOException, SQLException
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
            //link to sendCloud
            out.println("  <a href=\"" + request.getContextPath() + "/send-cloud/aws-s3/" + 
                    col[i].getHandle() + "/this" + "\">" + "sendCloudAmazon" + "</a>");
            //link to sendCloud all
            out.println("  <a href=\"" + request.getContextPath() + "/send-cloud/aws-s3/" + 
                    col[i].getHandle() + "/all" + "\">" + "sendAllCloudAmazon" + "</a>");
            //show items if collections contais
            if(items.containsKey(col[i].getID()))
                showItems(items.get(col[i].getID()));
            out.println("</li>");
        }
        out.println("</ul>");
    }
    
    //function to show the items and respective links
    void showItems(ItemIterator obj) throws IOException, SQLException
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
            out.println("  <a href=\"" + request.getContextPath() + "/backup/" + 
                    newObj.getHandle() + "\">" + "backup" + "</a>");
            //link to sendCloud
            out.println("  <a href=\"" + request.getContextPath() + "/send-cloud/aws-s3/" + 
                    newObj.getHandle() + "/this" + "\">" + "sendCloudAmazon" + "</a>");
            out.println("</li>");
        }
        out.println("</ul>");
    }
%>

<dspace:layout titlekey="jsp.admin-backup.title">

    <%
        //out.println("olá tudo bem!!! " + frase);
        
        setContext(out, request);
        
        showCommunities(com, subComMap, colMap, itemMap, backupComDone, backupColDone);
    %>           
        
    <br> <br>
    
</dspace:layout>