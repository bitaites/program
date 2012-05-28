/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.webui.servlet.mpinho;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.dspace.app.webui.mpinho.ActualContentManagement;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * Servlet to get files from amazon cloud.  
 * 
 * @author mpinho
 */
public class GetFileCloudServlet extends DSpaceServlet
{
    
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) 
            throws ServletException, IOException, SQLException
    {
        String server = null;
        String handle = null;
        String extraPathInfo = null;
        DSpaceObject dso = null;

        try
        {
            //examples:  "/aws-s3/123456789/1" or
            // "/aws-s3/123456789/1/this" or
            // "/aws-s3/123456789/1/all"
            String path = request.getPathInfo();

            // substring(1) is to remove initial '/'
            path = path.substring(1);

            // Extract the Handle
            int firstSlash = path.indexOf('/');
            int secondSlash = path.indexOf('/', firstSlash + 1);
            int thirdSlash = path.indexOf('/', secondSlash + 1);

            //extract info
            server = path.substring(0, firstSlash);
            handle = path.substring(firstSlash + 1, thirdSlash);
            extraPathInfo = path.substring(thirdSlash + 1);
            
            if (server == null || handle == null)
                return;
        }
        catch (NumberFormatException nfe)
        {
            // Leave handle as null
            return;
        }
        
        dso = HandleManager.resolveToObject(context, handle);
        
        //get DspaceObjet Type
        int type = dso.getType();
        //do the backup according the instructions
        ActualContentManagement act = new ActualContentManagement();
        switch(type)
        {
            case Constants.COMMUNITY:
                if(extraPathInfo.compareTo("this") == 0)
                    act.getCommunity(context, dso.getID(), true);
                else if(extraPathInfo.compareTo("all") == 0)
                    act.getCommunityAndChilds(context, dso.getID(), true);
                break;
            case Constants.COLLECTION:
                if(extraPathInfo.compareTo("this") == 0)
                    act.getCollection(context, dso.getID(), true);
                else if(extraPathInfo.compareTo("all") == 0)
                    act.getCollectionAndChilds(context, dso.getID(), true);
                break;
            case Constants.ITEM:
                act.getItem(context, dso.getID(), true);
                break;
            default:
                break;
        }
        
        //redirect to the view "admin-actualContent"
        String originalURL = request.getContextPath() + "/admin-actualContent";
        response.sendRedirect(response.encodeRedirectURL(originalURL));
    }
}
