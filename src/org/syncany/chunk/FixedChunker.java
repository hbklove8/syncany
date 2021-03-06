/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.chunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FixedChunker extends Chunker {
    public static final String DEFAULT_DIGEST_ALG = "SHA1";
	public static final String TYPE = "fixed";
	public static final String PROPERTY_SIZE = "size";

    private int chunkSize;
    private MessageDigest digest;
    private MessageDigest fileDigest;    
    private InputStream fileInputStream;
    private String checksumAlgorithm;
    
    /**
     * 
     * @param chunkSize in byte
     */
    public FixedChunker(int chunkSize) {
        this(chunkSize, DEFAULT_DIGEST_ALG);
    }
    
    /**
     * 
     * @param chunkSize in byte
     * @param checksumAlgorithm
     */
    public FixedChunker(int chunkSize, String checksumAlgorithm) {
        this.chunkSize = chunkSize;        
 
        try {
            this.digest = MessageDigest.getInstance(checksumAlgorithm);
            this.fileDigest = MessageDigest.getInstance(checksumAlgorithm);     
            
            this.fileDigest.reset();
            this.fileInputStream = null;
            
            this.checksumAlgorithm = checksumAlgorithm;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }        
    }
  
    @Override
    public Enumeration<Chunk> createChunks(File file) throws IOException {
        fileInputStream = new FileInputStream(file);
    	return new FixedChunkEnumeration(fileInputStream);
    }
    
	@Override
	public String getChecksumAlgorithm() {
		return checksumAlgorithm;
	}

    @Override
    public void close() {
    	try { fileInputStream.close(); }
    	catch (Exception e) { /* Not necessary */ }
    }

    @Override
    public String toString() {
        return "Fixed-"+chunkSize+"-"+digest.getAlgorithm();
    }

    public class FixedChunkEnumeration implements Enumeration<Chunk> {
        private InputStream in;           
        private byte[] buffer;
        private boolean closed;
        
        public FixedChunkEnumeration(InputStream in) {
            this.in = in;
            this.buffer = new byte[chunkSize];
            this.closed = false;
        }
        
        @Override
        public boolean hasMoreElements() {
            if (closed) {
                return false;
            }
            
            try {
                //System.out.println("fis ="+fis.available());
                return in.available() > 0;
            }
            catch (IOException ex) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "Error while reading from file input stream.", ex);
                }
                
                return false;
            }
        }

        @Override
        public Chunk nextElement() {    
            try {
                int read = in.read(buffer);
                
                if (read == -1) {
                    return null;
                }
                
                // Close if this was the last bytes
                if (in.available() == 0) {
                    in.close();
                    closed = true;
                }
                
                // Chunk checksum
                digest.reset();
                digest.update(buffer, 0, read);
                
                // File checksum
                fileDigest.update(buffer, 0, read);                                
                byte[] fileChecksum = (closed) ? fileDigest.digest() : null;

                // Create chunk
                return new Chunk(digest.digest(), buffer, read, fileChecksum);
            } 
            catch (IOException ex) {                
                logger.log(Level.SEVERE, "Error while retrieving next chunk.", ex);
                return null;
            }
        }
        
    }
}

