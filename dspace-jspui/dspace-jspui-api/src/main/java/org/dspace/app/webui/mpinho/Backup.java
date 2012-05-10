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
import java.util.ArrayList;
import java.util.List;
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
 * @author bitaites
 */
public class Backup {
    
    private String path = "/home/bitaites/Desktop/backupfiles/";
    
        /**
    * Return the name of the files of some type.
    * 
    * @param type
    *            DSpace type object
    * 
    * @return the list of names
    */
    private List<String> listFiles(String type)
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
    }
    
    /**
    * See if a file exists in the default path.
    * 
    * @param name
    *            Name of the file
    * 
    * @return true if exists or false if not exists 
    */
    private Boolean existFile(String name) 
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
        
        Logbackup newLog = new Logbackup();
        newLog.deleteLogTable(context, ref, type);
        
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
    private String getFileNameObj(DSpaceObject obj)
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
    public List<String> listAllFiles()
    {   
        return this.listFiles(null);
    }
    
    /**
    * Return the names of the files in the 
    * default path related with communities.
    * 
    * @return the list of names
    */
    public List<String> listCommunities() 
    {
        return this.listFiles("COMMUNITY");
    }
    
    /**
    * Return the names of the files in the 
    * default path related with collections.
    * 
    * @return the list of names
    */
    public List<String> listCollections() 
    {
        return this.listFiles("COLLECTION");
    }
    
    /**
    * Return the names of the files in the 
    * default path related with items.
    * 
    * @return the list of names
    */
    public List<String> lisItems() 
    {
       return this.listFiles("ITEM");
    }
    
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
        //TODO - inserir aqui as restrições para o export
        Boolean val = export(context, Constants.ITEM, ref);
        
        return val;
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
    * See if the backup of a DSpaceObject is done.
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
        Logbackup logb = new Logbackup();
        //see if there is a modification registry in the db
        Boolean existLog = logb.existsLog(context, ref, type);
        
        //get the DSpaceObject
        DSpaceObject obj = this.getDSpaceObject(context, type, ref);
        
        //get the name of the backup file
        String filename = this.getFileNameObj(obj);
        
        //see if backup file exists
        Boolean existFile = this.existFile(filename);
        
        if(existFile == true && existLog == false)
            return true;
        else
            return false;
    }
    
}
