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
import org.dspace.app.webui.mpinho.ActualContentManagement;
import org.dspace.app.webui.mpinho.Backup;
import org.dspace.app.webui.mpinho.Replace;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

/**
 * Servlet to the view admin-backup.  
 * 
 * Show all the objects(communities, collections and items) and the backup options
 * 
 * @author mpinho
 */
public class ActualContentServlet extends DSpaceServlet
{
    
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
        //This will contain all the communityIDs that is possible to get the backup file
        Set<Integer> couldGetComFile = new HashSet<Integer>();
        //This will contain all the collectionIDs that is possible to get the backup file
        Set<Integer> couldGetColFile = new HashSet<Integer>();
        //This will contain all the itemIDs that is possible to get the backup file
        Set<Integer> couldGetItemFile = new HashSet<Integer>();
        //This will contain all the communityIDs that is possible to do replace
        Set<Integer> couldDoReplaceCom = new HashSet<Integer>();
        //This will contain all the collectionIDs that is possible to do replace
        Set<Integer> couldDoReplaceCol = new HashSet<Integer>();
        //This will contain all the itemIDs that is possible to do resplace
        Set<Integer> couldDoReplaceItem = new HashSet<Integer>();
        
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
            ActualContentManagement conCloud = new ActualContentManagement();
            
            //see the status backup of Collections
            if (collections.length != 0)
                backupColDone.addAll(objBackup.checkCollectionsBackup(context, collections));
            
            //see wich collections have the updated backup file in cloud
            if (collections.length != 0)
                cloudColExist.addAll(conCloud.checkCollectionsInCloud(context, collections));
            
            //see wich collections are possbile to get the backup file from cloud
            if (collections.length != 0)
                couldGetColFile.addAll(conCloud.checkPossibleCollectionsGet(context, collections));
            
            //see wich collectionIDs are possible to resplace
            Replace replaceData = new Replace();
            if (collections.length != 0)
                couldDoReplaceCol.addAll(replaceData.checkCollectionsReplace(context, collections));
            
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
                
                //see wich items are possbile to get the backup file from cloud
                if (item.hasNext())
                    couldGetItemFile.addAll(conCloud.checkPossibleItemsGet(context, item));
                
                //see wich itemIDs are possible to replace
                if (item.hasNext())
                    couldDoReplaceItem.addAll(replaceData.checkItemsReplace(context, item));
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
        
        ActualContentManagement conCloud = new ActualContentManagement();
        
        //see wich communities have the updated backup file in cloud
        if (allCommunities.length != 0)
            cloudComExist.addAll(conCloud.checkCommunitiesInCloud(context, allCommunities));
        
        //see wich communities are possbile to get the backup file from cloud
        if (allCommunities.length != 0)
            couldGetComFile.addAll(conCloud.checkPossibleCommunitiesGet(context, allCommunities));
        
        //see wich communityIDs are possible to replace
        Replace replaceData = new Replace();
        if (allCommunities.length != 0)
            couldDoReplaceCom.addAll(replaceData.checkCommunitiesReplace(context, allCommunities));
        
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
        request.setAttribute("couldGetComFile", couldGetComFile);
        request.setAttribute("couldGetColFile", couldGetColFile);
        request.setAttribute("couldGetItemFile", couldGetItemFile);
        request.setAttribute("couldDoReplaceCom", couldDoReplaceCom);
        request.setAttribute("couldDoReplaceCol", couldDoReplaceCol);
        request.setAttribute("couldDoReplaceItem", couldDoReplaceItem);
        JSPManager.showJSP(request, response, "/admin-actualContent.jsp");
    }
    
}
