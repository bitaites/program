/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.mpinho;

import com.Ostermiller.util.MD5;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dspace.content.*;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;

/**
 * Class for Backup 
 * 
 * @author mpinho
 */
public class Backup {
    
    private String path = ConstantsMPinho.pathBackupFiles;
    
    /**
    * Return the MD5 of the file.
    * 
    * @param filename
    *            Name of the backup file
    * 
    * @return the MD5 of the file
    */
    public String getMD5File(String filename)
    {
        //get MD5 of the file
        try {
            return MD5.getHashString(new File(this.path+filename));
        } 
        catch (IOException ex) 
        {
            java.util.logging.Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    /**
    * Return the name of the files of some type.
    * 
    * @param type
    *            DSpace type object
    * 
    * @return the list of names
    */
    /*private List<String> listFiles(String type)
    {
        List <String> files = new ArrayList<String>();
        
        //get the default folder for the backups files
        File Folder = new File(path);
        //get all the files inside this folder
        File[] listOfFiles = Folder.listFiles();
        
        //for all files in the folder:
        for(int i=0; i<listOfFiles.length; i++)
        {
            if(listOfFiles[i].isFile()) 
            {
                //compare the name of the file with the type pretended
                if (type != null)
                {
                    String name = listOfFiles[i].getName();
                    String temp[] = name.split("@");
                
                    if(temp[0].compareTo(type) == 0)
                        files.add(listOfFiles[i].getName());
                }
                else
                    files.add(listOfFiles[i].getName());
            }
        }
        
        return files;
    }*/
    
    /**
    * See if a file exists in the default path.
    * 
    * @param name
    *            Name of the file
    * 
    * @return true if exists or false if not exists 
    */
    public Boolean existFile(String name) 
    {       
        File Folder = new File(path);
        
        File[] listOfFiles = Folder.listFiles();
        
        if(listOfFiles != null)
        { 
            //compare all the files name with the name received
            for(int i=0; i<listOfFiles.length; i++)
            {
                if(listOfFiles[i].isFile())
                    if(listOfFiles[i].getName().compareTo(name) == 0)
                        return true;
            }
        }
        
        return false;
    }
    
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
    
    /**
    * Do the backup of some community, collection or item
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
    * @return true if exports or false if not
    */
    private Boolean export(Context context, int type, int ref)
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
        
        //get file name
        String pathFile = path + this.getFileNameObj(obj);
        
        //see if exists file 
        boolean exists = (new File(pathFile)).exists();
        
        //if file exists, delete it
        if(exists == true)
            (new File(pathFile)).delete();
        
        File newFile = new File(pathFile);
        PackageParameters params = new PackageParameters();
        //params.addProperty("manifestOnly", "true");
        
        /*DSpaceAIPDisseminator pack = new DSpaceAIPDisseminator();
        try {
            pack.disseminate(context, obj, params, newFile);
        } 
        catch (Exception ex)
        {
            Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
            return false;    
        }*/
        
        PackageDisseminator dip = (PackageDisseminator) 
                PluginManager.getNamedPlugin(PackageDisseminator.class, "AIP");
        try {
            dip.disseminate(context, obj, params, newFile);
        } 
        catch (Exception ex) {
            Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //delete logtable if exists
        Logbackup newLog = new Logbackup();
        newLog.deleteLogTable(context, ref, type);
        
        BackupProcess newRegist = new BackupProcess();
        newRegist.updateProcessBackup(context, ref, type);
        
        return true;
    }
    
    /**
    * Return the file name to backup a DSpace Object (community, collection or item)
    * 
    * @param obj
    *            DSpaceObject
    * 
    * @return the file name
    */
    public String getFileNameObj(DSpaceObject obj)
    {
        String fileName = "";
        
        String handler = obj.getHandle().replace("/", "-");
        
        switch(obj.getType())
        {
            case Constants.COMMUNITY:
                fileName = "COMMUNITY@" + handler + ".zip"; 
                break;
            case Constants.COLLECTION:
                fileName = "COLLECTION@" + handler + ".zip";
                break;
            case Constants.ITEM:
                fileName = "ITEM@" + handler + ".zip";
                break;
            default:
                break;
        }
        
        return fileName;
    }
    
    /**
    * Return the names of the files in the default path.
    * 
    * @return the list of names
    */
    /*public List<String> listAllFiles()
    {   
        return this.listFiles(null);
    }*/
    
    /**
    * Return the names of the files in the 
    * default path related with communities.
    * 
    * @return the list of names
    */
    /*public List<String> listCommunities() 
    {
        return this.listFiles("COMMUNITY");
    }*/
    
    /**
    * Return the names of the files in the 
    * default path related with collections.
    * 
    * @return the list of names
    */
    /*public List<String> listCollections() 
    {
        return this.listFiles("COLLECTION");
    }*/
    
    /**
    * Return the names of the files in the 
    * default path related with items.
    * 
    * @return the list of names
    */
    /*public List<String> lisItems() 
    {
       return this.listFiles("ITEM");
    }*/
    
    /**
    * Do the backup of a community.
    * 
    * @param context
    *            DSpace context
    * 
    * @param ref
    *           ID of the community
    * 
    * @return true if backup correctly or false if the backup already exists 
    * or fails
    */
    public Boolean exportCommunity(Context context, Integer ref)
    {
        //see if backup already exists
        Boolean var = this.backupDone(context, ref, Constants.COMMUNITY);
        
        if(var == true)
            return false;
        else
        {
            Boolean val = export(context, Constants.COMMUNITY, ref);
            
            return val;
        }
    }
    
    /**
    * Do the backup of a collection.
    * 
    * @param context
    *            DSpace context
    * 
    * @param ref
    *           ID of the collection
    * 
    * @return true if backup correctly or false if the backup already exists 
    * or fails
    */
    public Boolean exportCollection(Context context, Integer ref) 
    {   
        //see if backup already exists
        Boolean var = this.backupDone(context, ref, Constants.COLLECTION);
        
        if(var == true)
            return false;
        else
        {
            Boolean val = export(context, Constants.COLLECTION, ref);;
            return val;
        }
    }
    
    /**
    * Do the backup of an item.
    * 
    * @param context
    *            DSpace context
    * 
    * @param ref
    *           ID of the item
    * 
    * @return true if backup correctly or false if the backup already exists 
    * or fails
    */
    public Boolean exportItem(Context context, Integer ref)
    {
        //see if backup already exists
        Boolean var = this.backupDone(context, ref, Constants.ITEM);
        
        if(var == true)
            return false;
        else
        {
            Boolean val = export(context, Constants.ITEM, ref);;
            return val;
        }
    }
    
    /**
    * Do the backup of a community and respective children DSpaceObjects.
    * 
    * @param context
    *            DSpace context
    * 
    * @param ref
    *           ID of the community
    * 
    * @return true if backup correctly or false if fails 
    * 
    */
    public Boolean exportCommunityAndChilds(Context context, Integer ref)
    {
        //export atual community
        exportCommunity(context, ref);
        
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
            Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //export all sub-communities
        if(subCommunities.length != 0)
        {
            for(int i=0; i<subCommunities.length; i++)
                exportCommunityAndChilds(context, subCommunities[i].getID());
        }
        
        //export all collections
        if(collections.length != 0)
        {
            for(int i=0; i<collections.length; i++)
                exportCollectionAndChilds(context, collections[i].getID());
        }
        
        return true;
    }
    
    /**
    * Do the backup of a collection and respective children DSpaceObjects.
    * 
    * @param context
    *            DSpace context
    * 
    * @param ref
    *           ID of the collection
    * 
    * @return true if backup correctly or false if fails 
    * 
    */
    public Boolean exportCollectionAndChilds(Context context, Integer ref)
    {
        //export atual collection
        exportCollection(context, ref);
        
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
            Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //export, one by one, each item
        try 
        {
            if(items.hasNext())
            {
                Item newObj = items.next();
                exportItem(context, newObj.getID());
            }
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
    }
    
    /**
    * See if the backup of a DSpaceObject is done and correctly exists.
    * DSpace Object could be community, collection or item
    * 
    * @param context
    *            DSpace context
    * 
    * @param ref
    *           ID of object in the database
    * 
    * @param type
    *           Type of the DSpaceObject
    * 
    * @return true if backup exists or false if not
    * 
    */
    public Boolean backupDone(Context context, int ref, int type)
    {          
        //see if object is a community or collection
        if(type == Constants.COMMUNITY || type == Constants.COLLECTION)
        {
            //see if there is a modification registry in the db
            Logbackup logb = new Logbackup();
            Boolean existLog = logb.existsLog(context, ref, type);
            
            //if modification has been detected return false
            if (existLog == true)
                return false;
        }

        //get the DSpaceObject
        DSpaceObject obj = this.getDSpaceObject(context, type, ref);

        //see if exist a regist of a backup in the table sthandfile
        BackupProcess backupProcess= new BackupProcess();
        Boolean existRegist = backupProcess.existRegist(context, obj.getHandle());

        //if doesn't exist a regist return false
        if(existRegist == false)
            return false;
                
        //get filename and see if backup file exists
        String filename = this.getFileNameObj(obj);
        Boolean existFile = this.existFile(filename);

        //if file doesn't exist return false
        if (existFile == false)
            return false;
        
        //get md5 of the local file
        String md5File = this.getMD5File(filename);
        //get the md5 saved int the last backup for this file
        String md5Save = backupProcess.getSavedMD5(context, obj.getHandle());

        //if the two md5 are differents return false because file is corrupted
        if(md5File.compareTo(md5Save) != 0)
            return false;

        if(type == Constants.ITEM)
        {
            //get the last modification date of the item
            Item item = null;
            try {
                item = Item.find(context, ref);
            } catch (SQLException ex) {
                Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
            }
            Date lastModification = item.getLastModified();
            
            //get the last backup date
            Date lastBackup = backupProcess.getLastBackupDate(context, obj.getHandle());
                    
            //see if some modification happens after a backup
            if(lastModification.after(lastBackup) == true)
                return false;
            else
                return true;
        }
        else
            //backup of a community or collection is done
            return true;  
    }
    
    /**
    * See if exists the Communities backup file and if it is updated.
    * 
    * @param context
    *            DSpace context
    * 
    * @param com
    *            array of Communities
    * 
    * @return integer set with all the IDs Item with existing backup done and updated
    * 
    */
    public Set<Integer> checkCommunitiesBackup(Context context, Community[] com)
    {
        //This will contain all the CommunityIDs with backup file correct
        Set<Integer> setInfo = new HashSet<Integer>();
        
        //do the operation for all communities
        for(int i=0; i<com.length; i++)
        {
            //check the backup file exists and is correct
            Boolean checkCorrect = this.backupDone(context, com[i].getID(), Constants.COMMUNITY);

            //add the ID community to set if correct
            if (checkCorrect == true)
                setInfo.add(com[i].getID());     
        }
        
        return setInfo;
    }
    
    /**
    * See if exists the Collections backup file and if it is updated.
    * 
    * @param context
    *            DSpace context
    * 
    * @param com
    *            array of Communities
    * 
    * @return integer set with all the IDs Item with existing backup done and updated
    * 
    */
    public Set<Integer> checkCollectionsBackup(Context context, Collection[] col)
    {
        //This will contain all the CollectionIDs with backup file correct
        Set<Integer> setInfo = new HashSet<Integer>();
        
        //do the operation for all collections
        for(int i=0; i<col.length; i++)
        {
            //check the backup file exists and is correct
            Boolean checkCorrect = this.backupDone(context, col[i].getID(), Constants.COLLECTION);

            //add the ID collection to set if correct
            if (checkCorrect == true)
                setInfo.add(col[i].getID());     
        }
        
        return setInfo;
    }
    
    /**
    * See if exists the Items backup file and if it is updated.
    * 
    * @param context
    *            DSpace context
    * 
    * @param com
    *            array of Items
    * 
    * @return integer set with all the IDs Item with existing backup done and updated
    * 
    */
    public Set<Integer> checkItemsBackup(Context context, ItemIterator items)
    {
        //This will contain all the ItemsIDs with backup file in cloud
        Set<Integer> setInfo = new HashSet<Integer>();
        
        try 
        {   
            //do the operation for all items
            while(items.hasNext() == true)
            {
                Item objItem = items.next();
                //check the backup file exists and is correct
                Boolean checkCorrect = this.backupDone(context, objItem.getID(), Constants.ITEM);

                //add the ID collection to set if correct
                if (checkCorrect == true)
                    setInfo.add(objItem.getID());
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
        }
 
        return setInfo;
    }
}
