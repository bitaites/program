/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.mpinho;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dspace.content.*;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;

/**
 *
 * @author mpinho
 */
public class Replace {
    
    private String path = ConstantsMPinho.pathBackupFiles;
    
    /**
    * Return the specific DSpaceObject.
    * Could be a community, collection or item.
    * 
    * @param context
    *            DSpace context
    * 
    * @param type 
    *           DSpace Object Type
    * 
    * @param ref
    *           ID of the object
    * 
    * @return the DSpaceObject if exists or null
    */
    private DSpaceObject getDSpaceObject(Context context, int type, int ref)
    {
        DSpaceObject obj = null;
        try {
            obj = DSpaceObject.find(context, type, ref);
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return obj;
    } 
    
    private Boolean replace(Context context, int type, int ref)
    {
        EPerson myPerson = null;
        
        //TODO change this - get current person logged
        try {
            myPerson = EPerson.findByEmail(context, "ei06128@fe.up.pt");
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //set the current user in the context
        if(myPerson != null)
            context.setCurrentUser(myPerson);
        
        //get DSpaceObject
        DSpaceObject obj = this.getDSpaceObject(context, type, ref);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return false;
        
        Backup backup = new Backup();
        String filename = backup.getFileNameObj(obj);
        
        //get file name
        String pathFile = path + filename;
        
        //parameters of ingester
        PackageParameters params = new PackageParameters();
        params.setRestoreModeEnabled(true);
        params.setReplaceModeEnabled(true);
        
        //get file to do the replace
        File theFile = new File(pathFile);
        
        PackageIngester sip = (PackageIngester) PluginManager
                    .getNamedPlugin(PackageIngester.class, "AIP");
        
        try {
            sip.replace(context, obj, theFile, params);
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(Replace.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        // if exist delete a regist from the table modifications
        Logbackup log = new Logbackup();
        log.deleteLogTable(context, ref, type);
        
        // put the replace file as backup file
        BackupProcess backupPro = new BackupProcess();
        backupPro.updateProcessBackup(context, ref, type);

        return true;
    }
    
    public Boolean replaceCommunity(Context context, Integer ref)
    {
        //see if replace is necessary or possible
        Boolean var = this.doReplace(context, ref, Constants.COMMUNITY);
        
        if(var == true)
        {
            Boolean val = this.replace(context, Constants.COMMUNITY, ref);
            return val;
        }
        else
            return false;
    }
    
    public Boolean replaceCollection(Context context, Integer ref)
    {
        //see if replace is necessary or possible
        Boolean var = this.doReplace(context, ref, Constants.COLLECTION);
        
        if(var == true)
        {
            Boolean val = this.replace(context, Constants.COLLECTION, ref);
            return val;
        }
        else
            return false;
    }
        
    public Boolean replaceItem(Context context, Integer ref)
    {
        //see if replace is necessary or possible
        Boolean var = this.doReplace(context, ref, Constants.ITEM);
        
        if(var == true)
        {
            Boolean val = this.replace(context, Constants.ITEM, ref);
            return val;
        }
        else
            return false;
    }
    
    public Boolean replaceCommunityAndChilds(Context context, Integer ref)
    {
        //replace atual community        
        this.replaceCommunity(context, ref);
        
        Community obj;
        Community[] subCommunities;
        Collection[] collections;
        
        //get community and the respective sub-communities and childs
        try 
        {
            obj = Community.find(context, ref);
            subCommunities = obj.getSubcommunities();
            collections = obj.getCollections();
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(Replace.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //replace all sub-communities
        if(subCommunities.length != 0)
        {
            for(int i=0; i<subCommunities.length; i++)
                replaceCommunityAndChilds(context, subCommunities[i].getID());
        }
        
        //replace all collections
        if(collections.length != 0)
        {
            for(int i=0; i<collections.length; i++)
                replaceCollectionAndChilds(context, collections[i].getID());
        }
        
        return true;
    }
        
    public Boolean replaceCollectionAndChilds(Context context, Integer ref)
    {
        //replace atual collection
        this.replaceCollection(context, ref);
        
        
        Collection obj;
        ItemIterator items;
        
        //get the items presents in the collection
        try 
        {
            obj = Collection.find(context, ref);
            items = obj.getAllItems();
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(Replace.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //replace, one by one, each item
        try 
        {
            if(items.hasNext())
            {
                Item newObj = items.next();
                replaceItem(context, newObj.getID());
            }
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(Replace.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
    }
    
    public Boolean doReplace(Context context, int ref, int type)
    {
        //get the DSpaceObject
        DSpaceObject obj = this.getDSpaceObject(context, type, ref);
        
        //if Object DSpace not exist, return false
        if(obj == null)
            return false;
        
        Backup backup = new Backup();
        
        //get backup filename of the object
        String filename = backup.getFileNameObj(obj);
        
        //see if backup file exists
        Boolean existFile = backup.existFile(filename);
        
        //if backup file does't exist return false
        if(existFile == false)
            return false;
        
        //get MD5 of local file
        String md5LocalFile = backup.getMD5File(filename);

        BackupProcess backupPro = new BackupProcess();
        //get MD5 of the last file sent to cloud
        String md5LastFileSent = backupPro.getSentMD5(context, obj.getHandle());
        //get md5 of the last backup file
        String md5LastBackup = backupPro.getSavedMD5(context, obj.getHandle());
        
        //see if local file is equal to the last file sent to cloud
        if(md5LocalFile.compareTo(md5LastFileSent) == 0)
        {
            //some change happened in DSpace Object and the backup has been done,
            //but someone get the last backup from cloud, and now a restore
            //can happens
            if(md5LocalFile.compareTo(md5LastBackup) != 0)
                return true;
        }
        else
        {
            //if md5 local file different from the last backup, file damaged, return false
            if(md5LocalFile.compareTo(md5LastBackup) != 0)
                return false;
        }
        
        //see if object is a community or collection
        if(obj.getType() == Constants.COMMUNITY || obj.getType() == Constants.COLLECTION)
        {
            Logbackup logMod = new Logbackup();
            Boolean existLog = logMod.existsLog(context, ref, type);
            
            //Some modification happens in obj DSpace after last backup
            if(existLog == true)
                return true;
            else
                return false;
        }
        else
        {
            try 
            {
                //get respective Item
                Item item = Item.find(context, ref);
                //get the last modification of the item
                Date lastModification = item.getLastModified();
                //get the last backup date
                Date lastBackup = backupPro.getLastBackupDate(context, obj.getHandle());
                
                //if modification happened after the backup, return true
                if(lastModification.after(lastBackup) == true)
                    return true;
                else
                    return false;
                
            } catch (Exception ex) {
                Logger.getLogger(Replace.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            
        }
    }
    
    public Set<Integer> checkCommunitiesReplace(Context context, Community[] com)
    {
        //This will contain all the CommunityIDs where is possible to do replace
        Set<Integer> setInfo = new HashSet<Integer>();
        
        //do the operation for all communities
        for(int i=0; i<com.length; i++)
        {
            //check if is possible or necessary do a replace 
            Boolean checkReplace = this.doReplace(context, com[i].getID(), Constants.COMMUNITY);

            //add the ID community to set if correct
            if (checkReplace == true)
                setInfo.add(com[i].getID());     
        }
        
        return setInfo;
    }
        
    public Set<Integer> checkCollectionsReplace(Context context, Collection[] col)
    {
        //This will contain all the CollectionIDs where is possible to do replace
        Set<Integer> setInfo = new HashSet<Integer>();
        
        //do the operation for all collections
        for(int i=0; i<col.length; i++)
        {
            //check if is possible or necessary do a replace 
            Boolean checkReplace = this.doReplace(context, col[i].getID(), Constants.COLLECTION);

            //add the ID community to set if correct
            if (checkReplace == true)
                setInfo.add(col[i].getID());     
        }
        
        return setInfo;
    }
            
    public Set<Integer> checkItemsReplace(Context context, ItemIterator items)
    {
        //This will contain all the ItemsIDs where is possible to do replace
        Set<Integer> setInfo = new HashSet<Integer>();
        
        try 
        {   
            //do the operation for all items
            while(items.hasNext() == true)
            {
                Item objItem = items.next();
                //check the backup file exists and is correct
                Boolean checkReplace = this.doReplace(context, objItem.getID(), Constants.ITEM);

                //check if is possible or necessary do a replace
                if (checkReplace == true)
                    setInfo.add(objItem.getID());
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(Replace.class.getName()).log(Level.SEVERE, null, ex);
        }
 
        return setInfo;
    }
    
}
