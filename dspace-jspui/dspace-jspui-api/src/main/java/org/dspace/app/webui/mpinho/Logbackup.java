/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.webui.mpinho;

import java.sql.Statement;
import java.util.logging.Level;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Class to access logs of Backup in the database
 * 
 * @author mpinho
 */
public class Logbackup {
    
    private String table = ConstantsMPinho.tableModifications;
    
    /**
    * Verify if exists a registry about a modification in a respective 
    * collection or community
    * 
    * @param context
    *            DSpace context
    * 
    * @param type 
    *           DSpace Object Type (community or collection)
    * 
    * @param ref
    *           ID of the object
    * 
    * @return true if exists or false if not
    */
    public Boolean existsLog(Context context, int ref, int type)
    {
        //define query
        String query = "SELECT * FROM " + this.table + " WHERE object_id = '" + 
                String.valueOf(ref) + "' and type_object = '" + String.valueOf(type) + "'";
        
        //execute query in db
        TableRow row = null;
        try {
            row = DatabaseManager.querySingle(context, query);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //return the result
        if(row != null)
            return true;
        else
            return false;
    }
    
    /**
    * Update or Insert a regist in the table "logbackup" if a update happens
    * 
    * @param context
    *            DSpace context
    * 
    * @param type_object 
    *           DSpace Object Type (community or collection)
    * 
    * @param object_id
    *           ID of the object
    * 
    * @param handler
    *           Handler of the object
    * 
    * @param action
    *           Modification action: insert, update, delete
    * 
    */
    public void updateLogTable(Context context, int object_id, int type_object, 
            String handler, String action)
    {           
        //see if exists another modification in the table logsbackup related with this object
        String queryNew = "SELECT * FROM " + this.table + " WHERE object_id = ?" + " and type_object = ?";        
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
            sql = "UPDATE " + this.table + " SET action = '" + action + 
                    "', last_modification = NOW() WHERE " + this.table + "_id = " + 
                    row.getIntColumn("logbackup_id");
        else
            sql = "INSERT INTO " + this.table + " VALUES (getnextid('" + this.table +"'), " + 
                    object_id + ", " + type_object + ", '" + handler + "', '" + action + "', NOW())";
        
        //try to execute the sql in the db
        try 
        {         
            Statement stat = context.getDBConnection().createStatement();
            stat.executeUpdate(sql);
        } 
        catch (Exception ex) 
        {
            java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
    }
    
    /**
    * Delete a regist from the table
    * 
    * @param context
    *            DSpace context
    * 
    * @param type 
    *           DSpace Object Type (community or collection)
    * 
    * @param ref
    *           ID of the object
    * 
    * @return true if delete successful or false if not
    */
    public boolean deleteLogTable(Context context, int ref, int type)
    {
        //if regist doesn't exist, return false
        if(!existsLog(context, ref, type))
            return false;
        else
        {
            //define query to get the row to delete
            String query = "SELECT * FROM " + this.table + " WHERE object_id = " + 
                ref + " and type_object = " + type;
        
            //contact to db to find the row and delete it
            TableRow row = null;
            int count = 0;
            try {
                row = DatabaseManager.querySingle(context, query);
                row.setTable(this.table);
                count = DatabaseManager.delete(context, row);
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //row not deleted
            if(count == 0)
                return false;
            else
            {
                //commit changes to db
                try {
                    context.commit();
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(Logbackup.class.getName()).log(Level.SEVERE, null, ex);
                }
                return true;
            }
        }
    }
    
}
