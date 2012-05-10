/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.webui.servlet.mpinho;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.app.webui.mpinho.Backup;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageException;
import org.dspace.core.LogManager;


/**
 * Servlet for testing new page
 * 
 * @author Micael Pinho
 */
public class Testes extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(Testes.class);
    
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "view_testes", ""));
        
        File siteFile = null;
        String name = "Hello World";
        
        /*Backup obj = new Backup();
        List<String> listData = obj.listAllFiles();
        List<String> listCommunities = obj.listCommunities();
        List<String> listCollections = obj.listCollections();
        List<String> listItems = obj.lisItems();
        List<File> allFiles = obj.getAllFiles();
        List<File> fileCommunities = obj.getCommunities();
        List<File> fileCollections = obj.getCollections();
        List<File> fileItems = obj.getItems();
        
        Boolean val = obj.existSite();
        if (val == true)
            siteFile = obj.getSite();

        Boolean successDel = obj.delete("site.zip");
        
        Boolean teste1, teste2, teste3;
        Boolean teste = false;
        teste1 = obj.exportCommunity(context, 1);
        //teste2 = obj.exportCollection(context, 1);
        //teste3 = obj.exportItem(context, 1);
        //if (teste1 && teste2 && teste3)
        //    teste = true;
        
        teste = teste1;
        
        Boolean sucessAllColExport = false; 
        //sucessAllColExport = obj.exportCollectionAndChilds(context, 1);
        Boolean sucessAllComExport = false;
        //sucessAllComExport = obj.exportCommunityAndChilds(context, 3);
        */
        request.setAttribute("hello", name);
        /*request.setAttribute("listData", listData);
        request.setAttribute("listCommunities", listCommunities);
        request.setAttribute("listCollections", listCollections);
        request.setAttribute("listItems", listItems);
        request.setAttribute("allFiles", allFiles);
        request.setAttribute("fileCommunities", fileCommunities);
        request.setAttribute("fileCollections", fileCollections);
        request.setAttribute("fileItems", fileItems);
        request.setAttribute("val", val);
        request.setAttribute("siteFile", siteFile);
        request.setAttribute("successDel", successDel);
        request.setAttribute("extractDone", teste);
        request.setAttribute("sucessAllColExport", sucessAllColExport);
        request.setAttribute("sucessAllComExport", sucessAllComExport);*/
        
        JSPManager.showJSP(request, response, "/testes.jsp");
    }
}
