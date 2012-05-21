/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.webui.mpinho;

import com.Ostermiller.util.MD5;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Class to access status of backup process registed in the database.
 * 
 * See all the information in the table sthanfile
 * 
 * @author bitaites
 */
public class BackupProcess {
    
    /** log4j category */
    private static Logger log = Logger.getLogger(BackupProcess.class);
    
    private String path = "/home/bitaites/Desktop/backupfiles/";
    
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
            java.util.logging.Logger.getLogger(Backup.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return obj;
    }
    
    public void updateProcessBackup(Context context, int object_id, int type_object)
    {
        //get the DSpace Object to get the filename
        DSpaceObject obj = this.getDSpaceObject(context, type_object, object_id);

        //get the handler object
        String handler = obj.getHandle();
        
        //get the filename corresponding with the object
        Backup newBackup = new Backup();
        String pathFile = this.path + newBackup.getFileNameObj(obj);

        //get MD5 of the file
        String hash;
        try {
            hash = MD5.getHashString(new File(pathFile));
        } 
        catch (IOException ex) 
        {
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
            
        //see if exists some regist in the table sthanfile related with this object
        String queryNew = "SELECT * FROM sthanfile WHERE object_id = ?" + " and type_object = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, object_id, type_object);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        String sql;
            
        //chose between update or insert regist in table logbackup
        if(row != null)
        {
            sql = "UPDATE sthanfile SET md5 = '" + hash + 
                    "', last_backup = NOW() WHERE sthanfile_id = " + 
                    row.getIntColumn("sthanfile_id");
        }        
        else
        {
            sql = "INSERT INTO sthanfile VALUES (getnextid('sthanfile'), " + 
                    object_id + ", " + type_object + ", '" + handler + "', NOW(), '" 
                    + hash + "')";
        }
        
        //try to execute the sql in the db
        try 
        {         
            Statement stat = context.getDBConnection().createStatement();
            stat.executeUpdate(sql);
            context.commit();
        } 
        catch (Exception ex) {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void updateProcessSendCloud(Context context, String handler, String md5, String etag)
    {  
        String sql = "UPDATE sthanfile SET etag = '" + etag + 
                    "', last_sendcloud = NOW(), " +
                    "md5_sent = '" + md5 + "' WHERE handle = '" + handler + "'";
        
        //try to execute the sql in the db
        try 
        {         
            Statement stat = context.getDBConnection().createStatement();
            stat.executeUpdate(sql);
            context.commit();
        } 
        catch (Exception ex) {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public String getSavedMD5(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM sthanfile WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (row != null)
            return row.getStringColumn("md5");
        else
            return null;
    }
    
    public Boolean existRegist(Context context, String handler)
    {
        //define query
        String queryNew = "SELECT * FROM sthanfile WHERE handle = ?";
        
        //execute query in db
        TableRow row = null;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //return the result
        if(row != null)
            return true;
        else
            return false;
    }
    
    public Date getLastBackupDate(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM sthanfile WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (row != null)
            return row.getDateColumn("last_backup");
        else
            return null;
    }
    
    public Date getLastSendCloudDate(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM sthanfile WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (row != null)
            return row.getDateColumn("last_sendcloud");
        else
            return null;
    }
    
    public Boolean equalsFiles(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM sthanfile WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        String md5_backup = row.getStringColumn("md5");
        String md5_sentCloud = row.getStringColumn("md5_sent");
        
        if (md5_backup.compareTo(md5_sentCloud) == 0)
            return true;
        else
            return false;
    }
    
}
