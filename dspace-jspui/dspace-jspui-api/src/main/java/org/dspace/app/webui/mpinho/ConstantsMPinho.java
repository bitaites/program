/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.mpinho;

/**
 * Class with constants. 
 * Change it with careful!!!
 * 
 * @author mpinho
 */
public class ConstantsMPinho 
{
    /** Destination folder of backup files */
    public static final String pathBackupFiles = 
            "/home/bitaites/Desktop/backupfiles/";
    
    /** Identity of the account Amazon s3 */
    public static final String identityCloudAmazon = "AKIAJ7U22TYN64UZZGTA";
    
    /** Password to access the services of Amazon s3 */
    public static final String passCloudAmazon = 
            "yKOuLVYtF1i79A5r1Ab2ZkRZezu4x2LFKT93CvzE";
    
    /** Container in the service Amazon s3*/
    public static final String containerCloudAmazon = "mpinho-dspace";
    
    /** Name of the table responsible for the registry of modifications 
     * in communities and collections*/
    public static final String tableModifications = "logbackup";
    
    /** Name of the table responsible for the registry of backups and sent to cloud*/
    public static final String tableBackupRegistry = "sthanfile";
}
