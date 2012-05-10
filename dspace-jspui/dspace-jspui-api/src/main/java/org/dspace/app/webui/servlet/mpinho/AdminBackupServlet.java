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

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.dspace.app.webui.mpinho.Backup;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.ItemIterator;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Servlet to the view admin-backup.  
 * 
 * Show all the objects(communities, collections and items) and the backup options
 * 
 * @author bitaites
 */
public class AdminBackupServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AdminBackupServlet.class);
    
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) 
            throws ServletException, IOException, SQLException
    {        
        // This will map communityIDs to arrays of collections
        Map<Integer, Collection[]> colMap = new HashMap<Integer, Collection[]>();
        // This will map communityIDs to arrays of sub-communities
        Map<Integer, Community[]> subComMap = new HashMap<Integer, Community[]>();
        // This will map collectionIDs to iterator of items
        Map<Integer, ItemIterator> itemMap = new HashMap<Integer, ItemIterator>();
        //This will contain all the communityIDs with backup done
        Set<Integer> backupComDone = new HashSet<Integer>();
        //This will contain all the collectionIDs with backup done
        Set<Integer> backupColDone = new HashSet<Integer>();
        
        //get top communities
        Community[] communities = Community.findAllTop(context);
        //get all comunities
        Community[] allCommunities = Community.findAll(context);
        
        //for all communities get sub-communities, collections and the respective items
        //also, see wich communities and collections has backup done
        for(int i=0; i<allCommunities.length; i++)
        {
            Integer comID = Integer.valueOf(allCommunities[i].getID());
            
            //see the status backup of Community
            Backup objDis = new Backup();
            if(objDis.backupDone(context, comID, Constants.COMMUNITY) == true)
                backupComDone.add(comID);
            
            //get collections
            Collection[] collections = allCommunities[i].getCollections();
            if (collections.length != 0)
                colMap.put(comID, collections);
            
            //see the status backup of Collections
            for(int j=0; j<collections.length; j++)
            {
                if(objDis.backupDone(context, collections[j].getID(), Constants.COLLECTION) 
                        == true)
                    backupColDone.add(collections[j].getID());
            }
            
            //get items
            for(int j=0; j<collections.length; j++)
            {
                ItemIterator item = collections[j].getItems();
                if(item.hasNext())
                    itemMap.put(collections[j].getID(), item);
            }
            
            //get sub-communities
            Community[] subCommunities = allCommunities[i].getSubcommunities();
            if (subCommunities.length != 0)
                subComMap.put(comID, subCommunities);
        }
        
        String hello = "hello";
        
        request.setAttribute("hello", hello);
        
        request.setAttribute("com", communities);
        request.setAttribute("subComMap", subComMap);
        request.setAttribute("colMap", colMap);
        request.setAttribute("itemMap", itemMap);
        request.setAttribute("backupComDone", backupComDone);
        request.setAttribute("backupColDone", backupColDone);
        
        JSPManager.showJSP(request, response, "/admin-backup.jsp");
    }
    
}
