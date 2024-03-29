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
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Class to access status of backup process registed in the database.
 * 
 * See all the information in the respective table
 * 
 * @author mpinho
 */
public class BackupProcess {
    
    private String path = ConstantsMPinho.pathBackupFiles;
    private String table = ConstantsMPinho.tableBackupRegistry;
    
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
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return obj;
    }
    
    /**
    * When a process of backup happens update register.
    * 
    * @param context
    *            DSpace context
    * 
    * @param object_id 
    *           ID of the DSpace Object
    * 
    * @param type_object
    *           Type of DSpace Object
    * 
    */
    public void updateProcessBackup(Context context, int object_id, int type_object)
    {
        //get the DSpace Object to get the filename
        DSpaceObject obj = this.getDSpaceObject(context, type_object, object_id);
        
        //if Object DSpace doesn't exist, return false
        if (obj == null)
            return;

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
        String queryNew = "SELECT * FROM " + this.table + " WHERE object_id = ?" + " and type_object = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, object_id, type_object);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        String sql;
            
        //chose between update or insert regist in table
        if(row != null)
        {
            sql = "UPDATE " + this.table + " SET md5 = '" + hash + 
                    "', last_backup = NOW() WHERE " + this.table + "_id = " + 
                    row.getIntColumn(this.table + "_id");
        }        
        else
        {
            sql = "INSERT INTO " + this.table + " VALUES (getnextid('" + this.table +"'), " + 
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
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
    * When one file is sent to cloud update register.
    * 
    * @param context
    *            DSpace context
    * 
    * @param handler
    *           Handler of the DSpace Object
    * 
    * @param md5
    *           MD5 of the file sent to cloud
    * 
    * @param etag
    *           ETag returned by the server cloud
    *
    */
    public void updateProcessSendCloud(Context context, String handler, String md5, String etag)
    {  
        String sql = "UPDATE " + this.table + " SET etag = '" + etag + 
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
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
        }   
    }
    
    /**
    * Get the saved MD5 of the backup file.
    * The save happens when the backup occurs.
    * 
    * @param context
    *            DSpace context
    * 
    * @param handler
    *           Handler of the DSpace Object
    * 
    * @return the saved MD5
    */
    public String getSavedMD5(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM " + this.table + " WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (row != null)
            return row.getStringColumn("md5");
        else
            return null;
    }
    
    /**
    * Get the MD5 of the last backup file sent to cloud.
    * The save happens when a sent to cloud occurs.
    * 
    * @param context
    *            DSpace context
    * 
    * @param handler
    *           Handler of the DSpace Object
    * 
    * @return the saved MD5 of the file sent to cloud
    */
    public String getSentMD5(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM " + this.table + " WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (row != null)
            return row.getStringColumn("md5_sent");
        else
            return null;
    }
    
    /**
    * See if exist a register of a backup operation.
    * The register involves a backup operation and maybe a send to cloud operation.
    * 
    * @param context
    *            DSpace context
    * 
    * @param handler
    *           Handler of the DSpace Object
    * 
    * @return true if register exists or false if not
    */
    public Boolean existRegist(Context context, String handler)
    {
        //define query
        String queryNew = "SELECT * FROM " + this.table + " WHERE handle = ?";
        
        //execute query in db
        TableRow row = null;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //return the result
        if(row != null)
            return true;
        else
            return false;
    }
    
    /**
    * Get the last backup date of the backup file.
    * 
    * @param context
    *            DSpace context
    * 
    * @param handler
    *           Handler of the DSpace Object
    * 
    * @return date when the last backup happened, or null if doesn't exist
    */
    public Date getLastBackupDate(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM " + this.table + " WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (row != null)
            return row.getDateColumn("last_backup");
        else
            return null;
    }
    
    /**
    * Get the last date when the backup has sent to cloud.
    * 
    * @param context
    *            DSpace context
    * 
    * @param handler
    *           Handler of the DSpace Object
    * 
    * @return date when the backup has sent to cloud, or null if doesn't exist
    */
    public Date getLastSendCloudDate(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM " + this.table + " WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (row != null)
            return row.getDateColumn("last_sendcloud");
        else
            return null;
    }
    
    /**
    * Get the ETag returned when the backup has sent to cloud.
    * 
    * @param context
    *            DSpace context
    * 
    * @param handler
    *           Handler of the DSpace Object
    * 
    * @return ETag returned when the backup has sent to cloud, or null if doesn't exist
    */
    public String getETag(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM " + this.table + " WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (row != null)
            return row.getStringColumn("etag");
        else
            return null;
    }
    
    /**
    * See if the last file sent to cloud is equal to the last one generated by the backup.
    * 
    * @param context
    *            DSpace context
    * 
    * @param handler
    *           Handler of the DSpace Object
    * 
    * @return true if files are equal, or false if not
    */
    public Boolean equalsFiles(Context context, String handler)
    {
        //get the row corresponding with specified handler
        String queryNew = "SELECT * FROM " + this.table + " WHERE handle = ?";        
        TableRow row;
        try {
            row = DatabaseManager.querySingle(context, queryNew, handler);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(BackupProcess.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        //get md5 of the last backup file
        String md5_backup = row.getStringColumn("md5");
        //get last md5 of the file sent to cloud
        String md5_sentCloud = row.getStringColumn("md5_sent");
        
        //compare if the two md5 are equal
        if (md5_backup.compareTo(md5_sentCloud) == 0)
            return true;
        else
            return false;
    }
    
}
