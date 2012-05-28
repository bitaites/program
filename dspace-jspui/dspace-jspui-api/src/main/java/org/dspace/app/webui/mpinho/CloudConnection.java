/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.mpinho;

import com.google.common.util.concurrent.ListenableFuture;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jclouds.blobstore.AsyncBlobStore;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import static org.jclouds.blobstore.options.ListContainerOptions.Builder.inDirectory;
import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;

/**
 *
 * @author mpinho
 */
public class CloudConnection 
{
    private BlobStoreContext connection = null;
    private String container = ConstantsMPinho.containerCloudAmazon;
    private String path = ConstantsMPinho.pathBackupFiles;
    
    /**
    * Make the connection to the cloud.
    */
    public void makeConnection(String cloud, String identity, String credential)
    {
        //set the properties of the connection
        Properties overrides = new Properties();
        overrides.setProperty("jclouds.mpu.parallel.degree", "10"); 
        overrides.setProperty("aws-s3.identity", identity);
        overrides.setProperty("aws-s3.credential", credential);
        
        //make the connection
        this.connection = new BlobStoreContextFactory().createContext(cloud, overrides);
    }
    
    /**
    * Close the connection to cloud.
    */
    public void closeConnection()
    {
        this.connection.close();
    }
    
    /**
    * Get actual connection to cloud.
    */
    public BlobStoreContext getConnection()
    {
        return this.connection;
    }
    
    /**
    * Get some information of some kind of files preserved in the cloud.
    * Get the name and ETag of the correct kind files, that are present in the
    * respective folder.
    * Return the info in a map<handler, ETag>
    * 
    * @param type
    *           type of files to get info 
    * 
    * @return a map containing the handler and the ETag of the files (map<handler, ETag>)
    */
    public Map<String, String> getInfoFilesIn(int type)
    {
        //define the connection type
        AsyncBlobStore blobStore = this.connection.getAsyncBlobStore();
        
        //create or open the container
        try {
            blobStore.createContainerInLocation(null, container).get();
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        String folder = null;
        
        if (type == Constants.COMMUNITY)
            folder = "COMMUNITY";
        else if (type == Constants.COLLECTION)
            folder = "COLLECTION";
        else if (type == Constants.ITEM)
            folder = "ITEM";
            
        //get all the files that exist in the folder
        PageSet<? extends StorageMetadata> str;
        try {
                str = blobStore.list(this.container, inDirectory(folder)).get();
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        //create map to preserve files information
        Map<String,String> map = new HashMap<String, String>();
        
        //see all the data presented in the list
        Iterator it = str.iterator();
        while(it.hasNext())
        {
            //get the regist file
            StorageMetadata storage = (StorageMetadata) it.next();
            //see if regist is a BLOB
            if(storage.getType().toString().compareTo("BLOB") == 0)
            {
                //get file name
                String nameFile = storage.getName();
                int firstSlash = nameFile.indexOf('@');
                int secondSlash = nameFile.indexOf('.');
                //see if file name is the expected
                if(firstSlash == -1 || secondSlash == -1)
                        continue;
                nameFile = nameFile.substring(firstSlash+1, secondSlash);
                //get the respective handler str
                nameFile = nameFile.replace('-', '/');
                //get the ETag file
                String ETag = storage.getETag();
                //get the correct ETag
                ETag = ETag.substring(1, ETag.length()-1);
                //put the data in the map
                map.put(nameFile, ETag);
            }
        }
        return map;
    }
    
    /**
    * Get some information about all the files preserved in the cloud.
    * Get the name and ETag of the files.
    * Return the info in a map with handler has key and ETag has value.
    * 
    * @param type
    *           type of files to get info 
    * 
    * @return a map containing the handler and the ETag of the files map(handler, ETag)
    */
    public Map<String, String> getAllFilesInCloud()
    {
        //create map to preserve files information
        Map<String,String> map = new HashMap<String, String>();
        
        map.putAll(this.getInfoFilesIn(Constants.COMMUNITY));
        map.putAll(this.getInfoFilesIn(Constants.COLLECTION));
        map.putAll(this.getInfoFilesIn(Constants.ITEM));
        
        return map;
    }
    
    /**
    * Send a small file to cloud to be preserved.
    * Small files are files with less than 34 MB.
    * 
    * @param filename
    *            name of the file
    * 
    * @param input 
    *            path of the file
    * 
    * @return the ETag returned by the cloud
    */
    private String sendSmallFile(String filename, File input)
    {
        //define the connection type
        BlobStore blobStore = this.connection.getBlobStore();
        
        //create or open the container
        blobStore.createContainerInLocation(null, this.container);
        
        //get the object type of the file
        int slash = filename.indexOf('@');
        String typeObject = filename.substring(0, slash) + "/";
        
        //if exists file with equal name in destination, delete it
        blobStore.removeBlob(this.container, typeObject+filename);
        
        //create blob with the file to sendo to cloud
        Blob blob = null;
        try 
        {
            blob = blobStore.blobBuilder(typeObject+filename).payload(input).
                            contentType(MediaType.APPLICATION_OCTET_STREAM).
                            contentDisposition(filename).calculateMD5().build();
        } 
        catch (IOException ex) {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        //put blob in cloud
        return blobStore.putBlob(container, blob, multipart()); 
    }
    
    /**
    * Send a big file to cloud to be preserved.
    * Big files are files with more than 34 MB.
    * 
    * @param filename
    *            name of the file
    * 
    * @param input 
    *            path of the file
    * 
    * @return the ETag returned by the cloud
    */
    private String sendBigFile(String filename, File input)
    {
        //define the connection type
        AsyncBlobStore blobStore = this.connection.getAsyncBlobStore();
        
        //create or open the container
        try {    
            blobStore.createContainerInLocation(null, container).get();
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        //get the object type of the file
        int slash = filename.indexOf('@');
        String typeObject = filename.substring(0, slash) + "/";
        
        //if exists file with equal name in destination, delete it
        try {
            blobStore.removeBlob(this.container, typeObject+filename).get();
        } catch (Exception ex) {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        
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
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
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
    public String sendFile(Context context, String filename)
    {   
        //get file to send to cloud
        File input;
        String newPath = "file://" + this.path + filename;    
        try {
            input = new File(new URI(newPath));
        } 
        catch (URISyntaxException ex) 
        {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        String Etag;
        
        //see if file tam is less than 34MB
        if(input.length() < 35651584)
            Etag = this.sendSmallFile(filename, input);
        else
            Etag = this.sendBigFile(filename, input);
        
        return Etag;
    }
    
    /**
    * Get a file preserved in the cloud.
    * 
    * @param filename
    *            name of the file
    * 
    * @return true if file correctly got from cloud, or false if not
    */
    public Boolean getFile(String filename)
    {       
        //define the connection type
        AsyncBlobStore blobStore = this.connection.getAsyncBlobStore();
        
        //create or open the container
        try {
            blobStore.createContainerInLocation(null, container).get();
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //get type file
        int firstSlash = filename.indexOf('@');
        String fileLocation = filename.substring(0, firstSlash) + "/" + filename;
        
        //see if exists file with same name in destination folder
        boolean exists = (new File(this.path + filename)).exists();
        
        //if file exists in destination folder, delete it
        if(exists == true)
            (new File(this.path + filename)).delete();
        
        //get the respective file
        ListenableFuture<Blob> futureETag = blobStore.getBlob(container, fileLocation);
        
        //create output file
        OutputStream out = null;
        try {
            out = new FileOutputStream(this.path + filename);
        } 
        catch (FileNotFoundException ex) 
        {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        //write the file cloud to destination output
        try {
            futureETag.get().getPayload().writeTo(out);
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(ActualContentManagement.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
    }
    
}
