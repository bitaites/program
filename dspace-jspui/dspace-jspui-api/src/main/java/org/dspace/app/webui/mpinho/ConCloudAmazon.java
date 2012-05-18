/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.mpinho;

import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jclouds.blobstore.AsyncBlobStore;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart; 

/**
 * Class for Send Files to Cloud Amazon
 * 
 * @author bitaites
 */
public class ConCloudAmazon {
    
    private String path = "/home/bitaites/Desktop/backupfiles/";
    private String identity = "AKIAJ7U22TYN64UZZGTA";
    private String credential = "yKOuLVYtF1i79A5r1Ab2ZkRZezu4x2LFKT93CvzE";
    private String container = "mpinho-dspace";
    
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
    
    private String sendSmallFile(BlobStoreContext blobContext, String filename, File input)
    {
        //define the connection type
        BlobStore blobStore = blobContext.getBlobStore();
        
        //create or open the container
        blobStore.createContainerInLocation(null, this.container);
        
        //get the object type of the file
        int slash = filename.indexOf('@');
        String typeObject = filename.substring(0, slash) + "/";
        
        //create blob with the file to sendo to cloud
        Blob blob = null;
        try 
        {
            blob = blobStore.blobBuilder(typeObject+filename).payload(input).
                            contentType(MediaType.APPLICATION_OCTET_STREAM).
                            contentDisposition(filename).calculateMD5().build();
        } 
        catch (IOException ex) {
            Logger.getLogger(ConCloudAmazon.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        //put blob in cloud
        return blobStore.putBlob(container, blob, multipart()); 
    }
    
    private String sendBigFile(BlobStoreContext blobContext, String filename, File input)
    {
        //define the connection type
        AsyncBlobStore blobStore = blobContext.getAsyncBlobStore();
        
        //create or open the container
        try {    
            blobStore.createContainerInLocation(null, container).get();
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(ConCloudAmazon.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        //get the object type of the file
        int slash = path.indexOf('@');
        String typeObject = path.substring(0, slash) + "/";
        
        //create blob with the file to sendo to cloud
        Blob blob = blobStore.blobBuilder(typeObject+filename).payload(input).
                        contentType(MediaType.APPLICATION_OCTET_STREAM).
                        contentDisposition(filename).build();
        
        //put blob in cloud
        ListenableFuture<String> futureETag = blobStore.putBlob(container, blob, multipart());
        
        //wait for the response
        try {
            return futureETag.get();
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(ConCloudAmazon.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    private Boolean send(String filename)
    {
        //set the properties of the connection
        Properties overrides = new Properties();
        overrides.setProperty("jclouds.mpu.parallel.degree", "10"); 
        overrides.setProperty("aws-s3.identity", this.identity);
        overrides.setProperty("aws-s3.credential", this.credential);
        
        //make the connection
        BlobStoreContext blobContext = new BlobStoreContextFactory().createContext("aws-s3", overrides);
        
        //get file to send to cloud
        File input = null;
        String newPath = "file://" + this.path + filename;    
        try {
            input = new File(new URI(newPath));
        } 
        catch (URISyntaxException ex) 
        {
            Logger.getLogger(ConCloudAmazon.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        String Etag = null;
        
        //see if file tam is less than 34MB
        if(input.length() < 35651584)
            Etag = this.sendSmallFile(blobContext, filename, input);
        else
            Etag = this.sendBigFile(blobContext, filename, input);
        
        //close the cloud connection
        blobContext.close();
        
        return true;
    }
    
    public Boolean sendCommunity(Context context, Integer ref)
    {
        DSpaceObject obj = this.getDSpaceObject(context, Constants.COMMUNITY, ref);
        
        //see if last backup has sent to cloud
        Boolean var = this.sendDone(context, obj.getHandle());
        if (var == true)
            return false;
        
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
            return false;
        
        //get file name of the community backup
        String filename = backup.getFileNameObj(obj);
        
        //send file to cloud
        return this.send(filename);
    }
    
    public Boolean sendCollection(Context context, Integer ref)
    {
        DSpaceObject obj = this.getDSpaceObject(context, Constants.COLLECTION, ref);
        
        //see if last backup has sent to cloud
        Boolean var = this.sendDone(context, obj.getHandle());
        if (var == true)
            return false;
        
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
            return false;
        
        //get file name of the community backup
        String filename = backup.getFileNameObj(obj);
        
        //send file to cloud
        return this.send(filename);
    }
    
    public Boolean sendItem(Context context, Integer ref)
    {
        DSpaceObject obj = this.getDSpaceObject(context, Constants.ITEM, ref);
        
        //see if last backup has sent to cloud
        Boolean var = this.sendDone(context, obj.getHandle());
        if (var == true)
            return false;
        
        Backup backup = new Backup();
        
        //see if backup exists and is correct
        Boolean statBackup = backup.backupDone(context, ref, Constants.ITEM);
        
        //if backup doesn't exist, do it
        Boolean statOperation = false;
        if (statBackup == false)
            statOperation = backup.exportItem(context, ref);
        else
            statOperation = true;
        
        //if backup operation fails, return false
        if(statOperation == false)
            return false;
        
        //get file name of the community backup
        String filename = backup.getFileNameObj(obj);
        
        //send file to cloud
        return this.send(filename);
    }
    
    public Boolean sendCommunityAndChilds(Context context, Integer ref)
    {
        //send to cloud atual community
        sendCommunity(context, ref);
        
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
        
        //send to cloud all sub-communities and childs
        if(subCommunities.length != 0)
        {
            for(int i=0; i<subCommunities.length; i++)
                sendCommunityAndChilds(context, subCommunities[i].getID());
        }
        
        //send to cloud all collections and childs
        if(collections.length != 0)
        {
            for(int i=0; i<collections.length; i++)
                sendCollectionAndChilds(context, collections[i].getID());
        }
        
        return true;
    }
    
    public Boolean sendCollectionAndChilds(Context context, Integer ref)
    {
        //send to cloud atual collection
        sendCollection(context, ref);
        
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
        
        //send to cloud, one by one, each item
        try 
        {
            if(items.hasNext())
            {
                Item newObj = items.next();
                sendItem(context, newObj.getID());
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
    * See if the last backup of the object is in cloud Amazon.
    * DSpace Object could be community, collection or item
    * 
    * @param context
    *            DSpace context
    * 
    * @param handler
    *           handler of the DSpace Object
    * 
    * @return true if last backup is in the cloud or false if not
    * 
    */
    public Boolean sendDone(Context context, String handler)
    {   
        //compare in xml last_backup date with last_sendcloud date
        return false;
    }
            
}
