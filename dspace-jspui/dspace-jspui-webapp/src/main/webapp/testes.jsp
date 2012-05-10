<%-- 

    The contents of this file are subject to the license and copyright 
    detailed in the LICENSE and NOTICE files at the root of the source 
    tree and available online at 

    http://www.dspace.org/license/

--%> 
<%@page import="javax.swing.JButton"%>
<%-- 
    Document   : newteste
    Created on : Apr 17, 2012, 2:26:01 PM
    Author     : bitaites
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Map"%>
<%@page import="java.io.File"%>
<%@page import="org.dspace.content.packager.PackageDisseminator" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    String frase = (String) request.getAttribute("hello");
    /*Boolean val = (Boolean) request.getAttribute("val");
    File fileSite = (File) request.getAttribute("siteFile");
    List<String> listData = (List<String>) request.getAttribute("listData");
    List<String> communitiesData = (List<String>) request.getAttribute("listCommunities");
    List<String> collectionsData = (List<String>) request.getAttribute("listCollections");
    List<String> itemsData = (List<String>) request.getAttribute("listItems"); 
    List<File> allFiles = (List<File>) request.getAttribute("allFiles");
    List<File> communitiesFiles = (List<File>) request.getAttribute("fileCommunities");
    List<File> collectionsFiles = (List<File>) request.getAttribute("fileCollections");
    List<File> itemsFiles = (List<File>) request.getAttribute("fileItems");
    Boolean successDel = (Boolean) request.getAttribute("successDel");
    Boolean extractDone = (Boolean) request.getAttribute("extractDone");
    Boolean sucessAllColExport = (Boolean) request.getAttribute("sucessAllColExport");
    Boolean sucessAllComExport = (Boolean) request.getAttribute("sucessAllComExport");*/
    
%>

<dspace:layout titlekey="Testes de Backup">
 
    <%
        out.println("olá tudo bem!!! " + frase);
    %>
        
    <br> <br>
    
    <%--
        out.println("exists site backup: " + val);
        if (val == true)
            out.println("  " + fileSite.getName());
    %>
        
    <br> <br>
    
    <%
        out.println("data: ");
        out.println(listData.size() + "; ");
        for(int i=0; i<listData.size(); i++)
            out.println(listData.get(i) + ";");
    %>
    
    <br> <br>
    
    <%
        out.println("communities: ");
        out.println(communitiesData.size() + "; ");
        for(int i=0; i<communitiesData.size(); i++)
            out.println(communitiesData.get(i) + ";");
    %>
    
    <br> <br>
    
    <%
        out.println("collection: ");
        out.println(collectionsData.size() + "; ");
        for(int i=0; i<collectionsData.size(); i++)
            out.println(collectionsData.get(i) + ";");
    %>
    
    <br> <br>
    
    <%
        out.println("items: ");
        out.println(itemsData.size() + "; ");
        for(int i=0; i<itemsData.size(); i++)
            out.println(itemsData.get(i) + ";");
    %>
    
        <br> <br>
    
    <%
        out.println("all files: ");
        out.println(allFiles.size() + "; ");
        for(int i=0; i<allFiles.size(); i++)
            out.println(allFiles.get(i).getName() + "; ");
    %>
    
        <br> <br>
    
    <%
        out.println("files communities: ");
        out.println(communitiesFiles.size() + "; ");
        for(int i=0; i<communitiesFiles.size(); i++)
            out.println(communitiesFiles.get(i).getName() + "; ");
    %>
    
        <br> <br>
    
    <%
        out.println("files collections: ");
        out.println(collectionsFiles.size() + "; ");
        for(int i=0; i<collectionsFiles.size(); i++)
            out.println(collectionsFiles.get(i).getName() + "; ");
    %>
    
        <br> <br>
    
    <%
        out.println("files items: ");
        out.println(itemsFiles.size() + "; ");
        for(int i=0; i<itemsFiles.size(); i++)
            out.println(itemsFiles.get(i).getName() + "; ");
    %>
    
        <br> <br>
    
    <%
        out.println("Success delete?: " + successDel);
    %>
    
        <br> <br>
    
    <%
        out.println("Extract done?: " + extractDone);
    %>
    
        <br> <br>
        
    <%
        out.println("All collections export done?: " + sucessAllColExport);
    %>
    
        <br> <br>
        
    <%
        out.println("All communities export done?: " + sucessAllComExport);
    --%>

</dspace:layout>
