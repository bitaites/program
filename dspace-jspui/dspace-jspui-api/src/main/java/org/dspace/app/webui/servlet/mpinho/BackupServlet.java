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

import org.dspace.app.webui.servlet.mpinho.AdminBackupServlet;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.dspace.app.webui.mpinho.Backup;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * Servlet to run the commands of backup. 
 * 
 * Run all the command of backup presents in the view "admin-backup"
 * 
 * @author bitaites
 */
public class BackupServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AdminBackupServlet.class);
    
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) 
            throws ServletException, IOException, SQLException
    {
        String handle = null;
        String extraPathInfo = null;
        DSpaceObject dso = null;

        //examples:  "/123456789/1" or
        // "/123456789/1/this" or
        // "/123456789/1/all"
        String path = request.getPathInfo();
        
        if (path != null)
        {
            // substring(1) is to remove initial '/'
            path = path.substring(1);

            try
            {
                // Extract the Handle
                int firstSlash = path.indexOf('/');
                int secondSlash = path.indexOf('/', firstSlash + 1);

                if (secondSlash != -1)
                {
                    // We have extra path info
                    handle = path.substring(0, secondSlash);
                    extraPathInfo = path.substring(secondSlash);
                    extraPathInfo = extraPathInfo.substring(1);
                }
                else
                {
                    // The path is just the Handle
                    handle = path;
                }
            }
            catch (NumberFormatException nfe)
            {
                // Leave handle as null
            }
        }
        
        //get the respective DSpaceObject
        if (handle != null)
        {
            dso = HandleManager.resolveToObject(context, handle);
        }
        
        //get DspaceObjet Type
        int type = dso.getType();
        //do the backup according the instructions
        Backup act = new Backup();
        switch(type)
        {
            case Constants.COMMUNITY:
                if(extraPathInfo.compareTo("this") == 0)
                    act.exportCommunity(context, dso.getID());
                else if(extraPathInfo.compareTo("all") == 0)
                    act.exportCommunityAndChilds(context, dso.getID());
                break;
            case Constants.COLLECTION:
                if(extraPathInfo.compareTo("this") == 0)
                    act.exportCollection(context, dso.getID());
                else if(extraPathInfo.compareTo("all") == 0)
                    act.exportCollectionAndChilds(context, dso.getID());
                break;
            case Constants.ITEM:
                act.exportItem(context, dso.getID());
                break;
            default:
                break;
        }
        
        //redirect to the view "admin-backup"
        String originalURL = request.getContextPath() + "/admin-backup";
        response.sendRedirect(response.encodeRedirectURL(originalURL));  
    }
    
}
