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

import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.app.webui.mpinho.Backup;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageException;
import org.dspace.core.LogManager;
import org.jclouds.blobstore.AsyncBlobStore;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;                                                                                                                 



/**
 * Servlet for testing new page
 * 
 * @author Micael Pinho
 */
public class Testes extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(Testes.class);
    
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "view_testes", ""));
        
        /*String identity = "AKIAJ7U22TYN64UZZGTA";
        String credential = "yKOuLVYtF1i79A5r1Ab2ZkRZezu4x2LFKT93CvzE";
        String container = "mpinho-dspace";
        String dirCom = "com-backup/";
        String dirCol = "col-backup/";
        String dirIte = "ite-backup/"; */
        
        File siteFile = null;
        String name = "Hello World";
        
        //String identity = "AKIAJ7U22TYN64UZZGTA";
        //String credential = "yKOuLVYtF1i79A5r1Ab2ZkRZezu4x2LFKT93CvzE";
        
        /*Properties overrides = new Properties();
        overrides.setProperty("jclouds.mpu.parallel.degree", "10"); 
        overrides.setProperty("aws-s3.identity", identity);
        overrides.setProperty("aws-s3.credential", credential);
    
        try 
        {
            BlobStoreContext blobContext = new BlobStoreContextFactory().createContext("aws-s3", overrides);
            
            AsyncBlobStore blobStore = blobContext.getAsyncBlobStore();

            blobStore.createContainerInLocation(null, container).get();
         
            blobStore.createDirectory(container, dirCom).get();
            
            File input = new File(new URI("file:///home/bitaites/Desktop/backupfiles/COMMUNITY123456789.zip"));
            
            String oi = MediaType.APPLICATION_OCTET_STREAM;
            
            //Blob blob = blobStore.blobBuilder("lol.zip").payload(input).contentType(container)            
            
            Blob blob = blobStore.blobBuilder("COMMUNITY123456789.zip").payload(input).
                    contentType("application/zip").
                    contentDisposition("COMMUNITY123456789.zip").build();
            
            ListenableFuture<String> futureETag = blobStore.putBlob(container, blob, multipart());
            
            //asynchronously wait for the upload                                                                                                                                                   
            String eTag = futureETag.get(); 

            blobContext.close();
        } 
        catch (Exception ex) 
        {
            String oi = "oi";
            return;
        }
                
        /*Backup obj = new Backup();
        List<String> listData = obj.listAllFiles();
        List<String> listCommunities = obj.listCommunities();
        List<String> listCollections = obj.listCollections();
        List<String> listItems = obj.lisItems();
        List<File> allFiles = obj.getAllFiles();
        List<File> fileCommunities = obj.getCommunities();
        List<File> fileCollections = obj.getCollections();
        List<File> fileItems = obj.getItems();
        
        Boolean val = obj.existSite();
        if (val == true)
            siteFile = obj.getSite();

        Boolean successDel = obj.delete("site.zip");
        
        Boolean teste1, teste2, teste3;
        Boolean teste = false;
        teste1 = obj.exportCommunity(context, 1);
        //teste2 = obj.exportCollection(context, 1);
        //teste3 = obj.exportItem(context, 1);
        //if (teste1 && teste2 && teste3)
        //    teste = true;
        
        teste = teste1;
        
        Boolean sucessAllColExport = false; 
        //sucessAllColExport = obj.exportCollectionAndChilds(context, 1);
        Boolean sucessAllComExport = false;
        //sucessAllComExport = obj.exportCommunityAndChilds(context, 3);
        */
        request.setAttribute("hello", name);
        /*request.setAttribute("listData", listData);
        request.setAttribute("listCommunities", listCommunities);
        request.setAttribute("listCollections", listCollections);
        request.setAttribute("listItems", listItems);
        request.setAttribute("allFiles", allFiles);
        request.setAttribute("fileCommunities", fileCommunities);
        request.setAttribute("fileCollections", fileCollections);
        request.setAttribute("fileItems", fileItems);
        request.setAttribute("val", val);
        request.setAttribute("siteFile", siteFile);
        request.setAttribute("successDel", successDel);
        request.setAttribute("extractDone", teste);
        request.setAttribute("sucessAllColExport", sucessAllColExport);
        request.setAttribute("sucessAllComExport", sucessAllComExport);*/
        
        JSPManager.showJSP(request, response, "/testes.jsp");
    }
}
