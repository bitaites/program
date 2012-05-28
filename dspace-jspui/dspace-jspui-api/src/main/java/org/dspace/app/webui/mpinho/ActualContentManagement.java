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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dspace.content.Collection;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Class for doing the management of existing DSpace Objects.
 * 
 * @author mpinho
 */
public class ActualContentManagement {
    
    
    private String identity = ConstantsMPinho.identityCloudAmazon;
    private String credential = ConstantsMPinho.passCloudAmazon;
    private String path = ConstantsMPinho.pathBackupFiles;
    private Map<String,String> filesInCloud = new HashMap<String, String>();
    CloudConnection newCloudConnection = new CloudConnection();
    
    /**
    * Make the connection to the cloud.
    */
    private void makeConnection()
    {
        this.newCloudConnection.makeConnection("aws-s3", this.identity, 
                this.credential);
    }
    
    /**
    * Close the connection to cloud.
    */
    private void closeConnection()
    {
        this.newCloudConnection.closeConnection();;
        this.filesInCloud.clear();
    }
    
    /**
    * Return the MD5 of the file.
    * 
    * @param filename
    *            Name of the backup file
    * 
    * @return the MD5 of the file or null if fails
    */
    private String getMD5File(String filename)
    {
        //get MD5 of the file
        try {
            return MD5.getHashString(new File(this.path+filename));
        } 
        catch (IOException ex) 
        {
            java.util.logging.Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
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
        catch (SQLException ex) {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return obj;
    }
    
    /**
    * Send a file to cloud to be preserved.
    * 
    * @param context
    *            context DSpace
    * 
    * @param filename
    *            name of the file
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    private Boolean sendFileToCloud(Context context, String filename)
    {   
        //send file to cloud and receive the ETag
        String Etag = newCloudConnection.sendFile(context, filename);
                
        //if Etag is null, fails to send file to cloud, return false
        if (Etag == null)
            return false;
        
        //get the handler to be recognized in the db
        int firstSlash = filename.indexOf('@');
        int secondSlash = filename.indexOf('.');
        String handler = filename.substring(firstSlash+1, secondSlash);
        handler = handler.replace('-', '/');
        
        //get the correct etag
        Etag = Etag.substring(1, Etag.length()-1);
        
        //get md5 of the local file
        String md5 = this.getMD5File(filename);
        
        //regist the operation in the db
        BackupProcess newRegist = new BackupProcess();
        newRegist.updateProcessSendCloud(context, handler, md5, Etag);
        
        return true;
    }

    /**
    * Get a file preserved in the cloud.
    * 
    * @param filename
    *            name of the file
    * 
    * @return true if file correctly got from cloud, or false if not
    */
    private Boolean getFileFromCloud(String filename)
    {       
        Boolean val = this.newCloudConnection.getFile(filename);
        
        return val;
    }
    
    /**
    * Send a community backup file to cloud to be preserved.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the community
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean sendCommunity(Context context, Integer ref, 
            Boolean establishConnection)
    {
        //get Dspace Object to the respective community
        DSpaceObject obj = this.getDSpaceObject(context, Constants.COMMUNITY, ref);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return false;
        
        //if connection not maked
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COMMUNITY));
        }
        
        //see if last backup has sent to cloud
        Boolean var = this.sendDone(context, ref, Constants.COMMUNITY);
        if (var == true)
        {
            //if just one file to send close connection with cloud
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        Backup backup = new Backup();
        
        //see if backup exists and is correct
        Boolean statBackup = backup.backupDone(context, ref, Constants.COMMUNITY);
        
        //if backup doesn't exist, do it
        Boolean statOperation = false;
        if (statBackup == false)
            statOperation = backup.exportCommunity(context, ref);
        else
            statOperation = true;
        
        //if backup operation fails, return false
        if(statOperation == false)
        {
            //if just one file to send close connection with cloud
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        //get file name of the community backup
        String filename = backup.getFileNameObj(obj);
        
        //send file to cloud
        Boolean result = this.sendFileToCloud(context, filename);
        
        //if just one file to send close connection with cloud
        if (establishConnection == true)
                this.closeConnection();
        
        return result;
    }
    
    /**
    * Get a community backup file preserved in the cloud.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the community
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean getCommunity(Context context, Integer ref,
                            Boolean establishConnection)
    {
        DSpaceObject obj = this.getDSpaceObject(context, Constants.COMMUNITY, ref);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return false;
        
        //if just one file to get make connection with cloud
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COMMUNITY));
        }
        
        //see if it is possible and necessary get community backup from cloud
        Boolean var = this.couldGetFileFromCloud(context, ref, Constants.COMMUNITY);
        if (var == false)
        {
            //if just one file to send close connection with cloud
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        //get file name of the community backup
        Backup backup = new Backup();
        String filename = backup.getFileNameObj(obj);
        
        
        //get file from cloud
        Boolean result = this.getFileFromCloud(filename);
        
        //if just one file to send close connection with cloud
        if(establishConnection == true)
            this.closeConnection();
        
        return result;
        
    }
    
    /**
    * Send a collection backup file to cloud to be preserved.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the collection
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean sendCollection(Context context, Integer ref, 
            Boolean establishConnection)
    {
        DSpaceObject obj = this.getDSpaceObject(context, Constants.COLLECTION, ref);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return false;
        
        //if connection not maked
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COLLECTION));
        }
        
        //see if last backup has sent to cloud
        Boolean var = this.sendDone(context, ref, Constants.COLLECTION);
        if (var == true)
        {
            //if just one file to send close connection with cloud
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        Backup backup = new Backup();
        
        //see if backup exists and is correct
        Boolean statBackup = backup.backupDone(context, ref, Constants.COLLECTION);
        
        //if backup doesn't exist, do it
        Boolean statOperation = false;
        if (statBackup == false)
            statOperation = backup.exportCollection(context, ref);
        else
            statOperation = true;
        
        //if backup operation fails, return false
        if(statOperation == false)
        {
            //if just one file to send close connection with cloud
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        //get file name of the collection backup
        String filename = backup.getFileNameObj(obj);
        
        //send file to cloud
        Boolean result = this.sendFileToCloud(context, filename);
        
        //if just one file to send close connection with cloud
        if (establishConnection == true)
            this.closeConnection();
        
        return result;
    }
    
    /**
    * Get a collection backup file preserved in the cloud.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the collection
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean getCollection(Context context, Integer ref,
                            Boolean establishConnection)
    {
        DSpaceObject obj = this.getDSpaceObject(context, Constants.COLLECTION, ref);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return false;
        
        //if just one file to get make connection with cloud
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COLLECTION));
        }
        
        //see if it is possible and necessary get collection backup from cloud
        Boolean var = this.couldGetFileFromCloud(context, ref, Constants.COLLECTION);
        if (var == false)
        {
            //if just one file to send close connection with cloud
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        //get file name of the community backup
        Backup backup = new Backup();
        String filename = backup.getFileNameObj(obj);
        
        //get file from cloud
        Boolean result = this.getFileFromCloud(filename);
        
        //if just one file to send close connection with cloud
        if(establishConnection == true)
            this.closeConnection();
        
        return result;
    }
    
    /**
    * Send an item backup file to cloud to be preserved.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the item
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean sendItem(Context context, Integer ref, 
            Boolean establishConnection)
    {
        DSpaceObject obj = this.getDSpaceObject(context, Constants.ITEM, ref);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return false;
        
        //if connection not maked
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.ITEM));
        }
        
        //see if last backup has sent to cloud
        Boolean var = this.sendDone(context, ref, Constants.ITEM);
        if (var == true)
        {
            //if just one file to send close connection with cloud
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        Backup backup = new Backup();
        
        //see if backup exists and is correct
        Boolean statBackup = backup.backupDone(context, ref, Constants.ITEM);
        
        //if backup doesn't exist, do it
        Boolean statOperation;
        if (statBackup == false)
            statOperation = backup.exportItem(context, ref);
        else
            statOperation = true;
        
        //if backup operation fails, return false
        if(statOperation == false)
        {
            //if just one file to send close connection with cloud
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        //get file name of the community backup
        String filename = backup.getFileNameObj(obj);
        
        //send file to cloud
        Boolean result = this.sendFileToCloud(context, filename);
        
        //if just one file to send close connection with cloud
        if (establishConnection == true)
            this.closeConnection();
        
        return result;
    }
    
    /**
    * Get a item backup file preserved in the cloud.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the item
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean getItem(Context context, Integer ref,
                        Boolean establishConnection)
    {
        DSpaceObject obj = this.getDSpaceObject(context, Constants.ITEM, ref);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return false;
        
        //if just one file to get make connection with cloud
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.ITEM));
        }
        
        //see if it is possible and necessary get community backup from cloud
        Boolean var = this.couldGetFileFromCloud(context, ref, Constants.ITEM);
        if (var == false)
        {
            //if just one file to send close connection with cloud
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        //get file name of the community backup
        Backup backup = new Backup();
        String filename = backup.getFileNameObj(obj);
        
        //get file from cloud
        Boolean result = this.getFileFromCloud(filename);
        
        //if just one file to send close connection with cloud
        if(establishConnection == true)
            this.closeConnection();
        
        return result;
    }
    
    /**
    * Send a community backup file and respective children to cloud to be preserved.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the community
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean sendCommunityAndChilds(Context context, Integer ref, 
            Boolean establishConnection)
    {
        //if true make the connection available and get all files in community
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getAllFilesInCloud());
        }    
        //send to cloud atual community
        sendCommunity(context, ref, false);
        
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
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            //it means it is the first father in the order, so close connection
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        //send to cloud all sub-communities and childs
        if(subCommunities.length != 0)
        {
            for(int i=0; i<subCommunities.length; i++)
                sendCommunityAndChilds(context, subCommunities[i].getID(), false);
        }
        
        //send to cloud all collections and childs
        if(collections.length != 0)
        {
            for(int i=0; i<collections.length; i++)
                sendCollectionAndChilds(context, collections[i].getID(), false);
        }
        
        //it means it is the first father in the order, so close connection and clear map
        if (establishConnection == true)
            this.closeConnection();
        
        return true;
    }
    
    /**
    * Get a preserved community backup file and respective children from cloud.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the community
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean getCommunityAndChilds(Context context, Integer ref, 
            Boolean establishConnection)
    {
        //if true make the connection available
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COMMUNITY));
        }
        
        //get file community from cloud
        getCommunity(context, ref, false);
        
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
            //it means it is the first father in the order, so close connection
            if (establishConnection == true)
                this.closeConnection();
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //get from cloud all the respective files sub-communities and childs
        if(subCommunities.length != 0)
        {
            for(int i=0; i<subCommunities.length; i++)
                getCommunityAndChilds(context, subCommunities[i].getID(), false);
        }
        
        //get from cloud all files collections and childs
        if(collections.length != 0)
        {
            for(int i=0; i<collections.length; i++)
                getCollectionAndChilds(context, collections[i].getID(), false);
        }
        
        //it means it is the first father in the order
        if (establishConnection == true)
            this.closeConnection();
        
        return true;
    }
    
    /**
    * Send a collection backup file and respective children to cloud to be preserved.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the collection
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean sendCollectionAndChilds(Context context, Integer ref, 
            Boolean establishConnection)
    {
        //if first, make connection and get collection and item files preserved in cloud
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COLLECTION));
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.ITEM));
        }
        
        //send to cloud atual collection
        sendCollection(context, ref, false);
        
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
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            //it means it is the first father in the order, so close connection
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        //send to cloud, one by one, each item
        try 
        {
            if(items.hasNext())
            {
                Item newObj = items.next();
                sendItem(context, newObj.getID(), false);
            }
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            //it means it is the first father in the order, so close connection
            if (establishConnection == true)
                this.closeConnection();
            return false;
        }
        
        //it means it is the first father in the order
        if (establishConnection == true)
            this.closeConnection();

        return true;
    }
    
    /**
    * Get a preserved collection backup file and respective children from cloud.
    * 
    * @param context
    *            context DSpace
    * 
    * @param ref
    *            ID of the collection
    * 
    * @param establishConnection
    *            true if pretend establish connection to cloud
    * 
    * @return true if file correctly sent to cloud, or false if not
    */
    public Boolean getCollectionAndChilds(Context context, Integer ref, 
            Boolean establishConnection)
    {
        //if first, make connection
        if (establishConnection == true)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COLLECTION));
        }
        
        //get from cloud atual collection file
        getCollection(context, ref, false);
        
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
            //it means it is the first father in the order, so close connection
            if (establishConnection == true)
                this.closeConnection();
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //get from cloud, one by one, each item file
        try 
        {
            if(items.hasNext())
            {
                Item newObj = items.next();
                getItem(context, newObj.getID(), false);
            }
        } 
        catch (Exception ex) 
        {
            //it means it is the first father in the order, so close connection
            if (establishConnection == true)
                this.closeConnection();
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //it means it is the first father in the order, so close connection
        if (establishConnection == true)
            this.closeConnection();
        
        return true;
    }
    
    /**
    * See if the last backup done is updated and if it is in cloud.
    * DSpace Object could be community, collection or item
    * 
    * @param context
    *            DSpace context
    * 
    * @param ref
    *            ID of the object
    * 
    * @param type
    *            type of the object DSpace
    * 
    * @return true if the updated backup is in the cloud or false if not
    * 
    */
    private Boolean sendDone(Context context, int ref, int type)
    {   
        //see if object is a community or collection
        if(type == Constants.COMMUNITY || type == Constants.COLLECTION)
        {
            //see if there is a modification registry in the db
            Logbackup logbackup = new Logbackup();
            Boolean existLog = logbackup.existsLog(context, ref, type);
            
            //if modification has been detected return false
            if(existLog == true)
                return false;
        }
            
        //get the DSpaceObject
        DSpaceObject obj = this.getDSpaceObject(context, type, ref);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return false;

        //see if exist a regist of a backup in the table sthandfile
        BackupProcess backupProcess= new BackupProcess();
        Boolean existRegist = backupProcess.existRegist(context, obj.getHandle());

        //if doesn't exist a regist return false
        if (existRegist == false)
            return false;
        
        //see if object is an item
        if(type == Constants.ITEM)
        {
            //get the last modification date of the item
            Item item = null;
            try {
                item = Item.find(context, ref);
            } catch (SQLException ex) {
                Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            }
            Date lastModification = item.getLastModified();
            
            //get the last backup date
            Date lastBackup = backupProcess.getLastBackupDate(context, obj.getHandle());
            
            //see if some modification happens after a backup
            if(lastModification.after(lastBackup) == true)
                return false;
        }
        
        //get the last send cloud dat
        Date lastSendCloud = backupProcess.getLastSendCloudDate(context, obj.getHandle());
        
        //if doesn't exist a date relative to the last send to cloud return false
        if(lastSendCloud == null)
            return false;
        
        //get the last backup date
        Date lastBackup = backupProcess.getLastBackupDate(context, obj.getHandle());
        
        //verify if a new backup happened
        if (lastSendCloud.before(lastBackup) == true)
        {
            //see if new md5 file backup is equal to the last md5 file backup sent to cloud
            Boolean equalFiles = backupProcess.equalsFiles(context, obj.getHandle());
            
            //if equal files return true
            if (equalFiles == false)
                return false;
        }
        
        //see if file exists in cloud, if not returns false
        if(this.filesInCloud.containsKey(obj.getHandle()))
        {
            //see if ETag is correct, if not returns false
            if(this.filesInCloud.get(obj.getHandle()).compareTo(
                    backupProcess.getETag(context, obj.getHandle())) == 0)
                return true;
            else
                return false;
        }
        else
            return false;
    }
    
    /**
    * See if the Communities have the updated backup file in cloud.
    * 
    * @param context
    *            DSpace context
    * 
    * @param com
    *            array of Communities
    * 
    * @return integer set with all the IDs Communities that have the updated backup in cloud
    * 
    */
    public Set<Integer> checkCommunitiesInCloud(Context context, Community[] com)
    {
        //This will contain all the CommunityIDs with backup file in cloud
        Set<Integer> setInfo = new HashSet<Integer>();
        
        //if exist some community to evaluate make the connection and 
        //get communities backups files in cloud 
        if(com.length != 0)
        {
            this.makeConnection();;
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COMMUNITY));
        }
        
        //do the operation for all communities
        for(int i=0; i<com.length; i++)
        {
            //check the backup file has been sent to cloud
            Boolean checkCorrect = this.sendDone(context, com[i].getID(), 
                    Constants.COMMUNITY);

            //add the ID community to set if correct
            if (checkCorrect == true)
                setInfo.add(com[i].getID());     
        }
        
        //close the connection to cloud
        this.closeConnection();
        
        return setInfo;
    }
    
    /**
    * See if the Collection have the updated backup file in cloud.
    * 
    * @param context
    *            DSpace context
    * 
    * @param col
    *            array of Collections
    * 
    * @return integer set with all the IDs Collection that have the updated backup in cloud
    * 
    */
    public Set<Integer> checkCollectionsInCloud(Context context, Collection[] col)
    {
        //This will contain all the CollectionsIDs with backup file in cloud
        Set<Integer> setInfo = new HashSet<Integer>();
        
        //if exist some collection to evaluate make the connection and 
        //get collections backups files in cloud 
        if(col.length != 0)
        {
            this.makeConnection();
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COLLECTION));
        }
        
        //do the operation for all communities
        for(int i=0; i<col.length; i++)
        {
            //check the backup file has been sent to cloud
            Boolean checkCorrect = this.sendDone(context, col[i].getID(), 
                    Constants.COLLECTION);

            //add the ID collection to set if correct
            if (checkCorrect == true)
                setInfo.add(col[i].getID());     
        }
        
        //close the connection to cloud
        this.closeConnection();
        
        return setInfo;
    }
    
    /**
    * See if the Item have the updated backup file in cloud.
    * 
    * @param context
    *            DSpace context
    * 
    * @param items
    *            ItemIterator of Items
    * 
    * @return integer set with all the IDs Item that have the updated backup in cloud
    * 
    */
    public Set<Integer> checkItemsInCloud(Context context, ItemIterator items)
    {
        //This will contain all the ItemsIDs with backup file in cloud
        Set<Integer> setInfo = new HashSet<Integer>();
        
        try 
        {
            //if exist some item to evaluate make the connection and 
            //get items backups files in cloud
            if(items.hasNext() == true)
            {
                this.makeConnection();
                this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                        Constants.ITEM));
            }
            
            //do the operation for all items
            while(items.hasNext() == true)
            {
                Item objItem = items.next();
                //check the backup file has been sent to cloud
                Boolean checkCorrect = this.sendDone(context, objItem.getID(), 
                        Constants.ITEM);
                //add the ID collection to set if correct
                if (checkCorrect == true)
                    setInfo.add(objItem.getID());
            }
            
            //close the connection to cloud
            this.closeConnection();
            
        } catch (SQLException ex) {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
 
        return setInfo;
    }
    
    /**
    * See which communities are possible to get the backup file from cloud.
    * 
    * @param context
    *            DSpace context
    * 
    * @param com
    *            array of Communities
    * 
    * @return integer set with all the IDs Communities that are possible to get the backup file from cloud
    * 
    */
    public Set<Integer> checkPossibleCommunitiesGet(Context context, Community[] com)
    {
        //This will contain all the Communities IDs that backup files could be get from cloud
        Set<Integer> setInfo = new HashSet<Integer>();
        
        //if exist some community to evaluate make the connection and 
        //get communities backups files in cloud 
        if(com.length != 0)
        {
            this.makeConnection();;
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COMMUNITY));
        }
        else
            return setInfo;
        
        //do the operation for all communities
        for(int i=0; i<com.length; i++)
        {
            //check if it is possible and necessary to get a backup file from cloud
            Boolean checkCorrect = this.couldGetFileFromCloud(context, com[i].getID(), 
                    Constants.COMMUNITY);

            //add the ID community to set if correct
            if (checkCorrect == true)
                setInfo.add(com[i].getID());     
        }
        
        //close the connection to cloud
        this.closeConnection();
        
        return setInfo;
    }
    
    /**
    * See which collections are possible to get the backup file from cloud.
    * 
    * @param context
    *            DSpace context
    * 
    * @param com
    *            array of Collections
    * 
    * @return integer set with all the IDs Collections that are possible to get the backup file from cloud
    * 
    */
    public Set<Integer> checkPossibleCollectionsGet(Context context, Collection[] col)
    {
        //This will contain all the Communities IDs that backup files could be get from cloud
        Set<Integer> setInfo = new HashSet<Integer>();
        
        //if exist some community to evaluate make the connection and 
        //get communities backups files in cloud 
        if(col.length != 0)
        {
            this.makeConnection();;
            this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(
                    Constants.COLLECTION));
        }
        else
            return setInfo;
        
        //do the operation for all communities
        for(int i=0; i<col.length; i++)
        {
            //check if it is possible and necessary to get a backup file from cloud
            Boolean checkCorrect = this.couldGetFileFromCloud(context, col[i].getID(), 
                    Constants.COLLECTION);

            //add the ID community to set if correct
            if (checkCorrect == true)
                setInfo.add(col[i].getID());     
        }
        
        //close the connection to cloud
        this.closeConnection();
        
        return setInfo;
    }
    
    /**
    * See which items are possible to get the backup file from cloud.
    * 
    * @param context
    *            DSpace context
    * 
    * @param item
    *            iterator of items
    * 
    * @return integer set with all the IDs Items that are possible to get the backup file from cloud
    * 
    */
    public Set<Integer> checkPossibleItemsGet(Context context, ItemIterator items)
    {
        //This will contain all the Items IDs that backup files could be get from cloud
        Set<Integer> setInfo = new HashSet<Integer>();
        
        try 
        {
            //if exist some item to evaluate make the connection and 
            //get items backups files in cloud
            if(items.hasNext() == true)
            {
                this.makeConnection();
                this.filesInCloud.putAll(this.newCloudConnection.getInfoFilesIn(Constants.ITEM));
            }
            
            //do the operation for all items
            while(items.hasNext() == true)
            {
                Item objItem = items.next();
                //check if it is possible and necessary to get a backup file from cloud
                Boolean checkCorrect = this.couldGetFileFromCloud(context, objItem.getID(), Constants.ITEM);
                //add the ID collection to set if correct
                if (checkCorrect == true)
                    setInfo.add(objItem.getID());
            }
            
            //close the connection to cloud
            this.closeConnection();
            
        } catch (SQLException ex) {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return setInfo;
    }
    
    /**
    * See if is necessary and possible to get the backup file from cloud.
    * 
    * @param context
    *            DSpace context
    * 
    * @param ref
    *            ID of the object
    * 
    * @param type
    *            type of the object DSpace
    * 
    * @return true if it is necessary or possible to get the backup from cloud or false if not
    * 
    */
    private Boolean couldGetFileFromCloud(Context context, int ref, int type)
    {
        //get the corresponding Dspace Object
        DSpaceObject obj = this.getDSpaceObject(context, type, ref);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return false;
        
        //see if backup file exists in cloud, if not return false
        if (this.filesInCloud.containsKey(obj.getHandle()))
        {
            BackupProcess backupPro = new BackupProcess();
            String ETagSaved = backupPro.getETag(context, obj.getHandle());
            
            //see if ETag of file in cloud, has equal to the last send, if not return false
            if (ETagSaved.compareTo(this.filesInCloud.get(obj.getHandle())) == 0)
            {
                //see if exists backup file locally
                Backup backup = new Backup();
                String filename = backup.getFileNameObj(obj);
                Boolean existLocalFile = backup.existFile(filename);
                
                //if not exist file locally return true
                if (existLocalFile == true)
                {
                    //get MD5 of local file
                    String md5LocalFile = backup.getMD5File(filename);
                    //get MD5 of the last file sent to cloud
                    String md5FileSentCloud = backupPro.getSentMD5(context, 
                            obj.getHandle());
                    
                    //if files equal, there is no necessity to get file from cloud, then return false
                    if(md5LocalFile.compareTo(md5FileSentCloud) == 0)
                        return false;
                    else
                        return true;
                }
                else
                    return true;
            }
            else
                return false;
        }
        else
            return false;
    }
           
}
