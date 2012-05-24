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
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.dspace.app.webui.mpinho.Backup;
import org.dspace.app.webui.mpinho.ConCloudAmazon;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.content.Collection;
import org.dspace.content.*;
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
        //This will contain all the itemIDs with backup done
        Set<Integer> backupItemDone = new HashSet<Integer>();
        //This will contain all the communityIDs with updated backup file in cloud
        Set<Integer> cloudComExist = new HashSet<Integer>();
        //This will contain all the collectionIDs with updated backup file in cloud
        Set<Integer> cloudColExist = new HashSet<Integer>();
        //This will contain all the itemIDs with updated backup file in cloud
        Set<Integer> cloudItemExist = new HashSet<Integer>();
        
        //get top communities
        Community[] communities = Community.findAllTop(context);
        //get all comunities
        Community[] allCommunities = Community.findAll(context);
        
        //for all communities get sub-communities, collections and the respective items
        //also, see wich collections and items has backup done
        for(int i=0; i<allCommunities.length; i++)
        {
            Integer comID = Integer.valueOf(allCommunities[i].getID());
            
            //get collections
            Collection[] collections = allCommunities[i].getCollections();
            if (collections.length != 0)
                colMap.put(comID, collections);
            
            Backup objBackup = new Backup();
            ConCloudAmazon conCloud = new ConCloudAmazon();
            
            //see the status backup of Collections
            if (collections.length != 0)
                backupColDone.addAll(objBackup.checkCollectionsBackup(context, collections));
            
            //see wich collections have the updated backup file in cloud
            if (collections.length != 0)
                cloudColExist.addAll(conCloud.checkCollectionsInCloud(context, collections));
            
            //get items
            for(int j=0; j<collections.length; j++)
            {
                ItemIterator item = collections[j].getItems();
                if(item.hasNext())
                    itemMap.put(collections[j].getID(), item);
                
                //see the status backup of Items
                if (item.hasNext())
                    backupItemDone.addAll(objBackup.checkItemsBackup(context, item));
                
                //see wich items have the updated backup file in cloud
                if (item.hasNext())
                    cloudItemExist.addAll(conCloud.checkItemsInCloud(context, item));
            }
            
            //get sub-communities
            Community[] subCommunities = allCommunities[i].getSubcommunities();
            if (subCommunities.length != 0)
                subComMap.put(comID, subCommunities);
        }
        
        //see the status backup of all Communities
        Backup objBackup = new Backup();
        if (allCommunities.length != 0)
            backupComDone.addAll(objBackup.checkCommunitiesBackup(context, allCommunities));
        
        //see wich collections have the updated backup file in cloud
        ConCloudAmazon conCloud = new ConCloudAmazon();
        if (allCommunities.length != 0)
            cloudComExist.addAll(conCloud.checkCommunitiesInCloud(context, allCommunities));
            
        
        String hello = "hello";
        
        request.setAttribute("hello", hello);
        
        request.setAttribute("com", communities);
        request.setAttribute("subComMap", subComMap);
        request.setAttribute("colMap", colMap);
        request.setAttribute("itemMap", itemMap);
        request.setAttribute("backupComDone", backupComDone);
        request.setAttribute("backupColDone", backupColDone);
        request.setAttribute("backupItemDone", backupItemDone);
        request.setAttribute("cloudComExist", cloudComExist);
        request.setAttribute("cloudColExist", cloudColExist);
        request.setAttribute("cloudItemExist", cloudItemExist);
        JSPManager.showJSP(request, response, "/admin-backup.jsp");
    }
    
}
