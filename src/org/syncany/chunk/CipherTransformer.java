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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.syncany.crypto.CipherSession;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.MultiCipherInputStream;
import org.syncany.crypto.MultiCipherOutputStream;

/**
 *
 * @author pheckel
 */
public class CipherTransformer extends Transformer {
	public static final String TYPE = "cipher";
	public static final String PROPERTY_CIPHER_SPECS = "cipherspecs";
	public static final String PROPERTY_PASSWORD = "password";
	
	private List<CipherSpec> cipherSpecs;
	private CipherSession cipherSession;
	
	public CipherTransformer() {
		this.cipherSpecs = new ArrayList<CipherSpec>();
		this.cipherSession = null;
	}
	
    public CipherTransformer(List<CipherSpec> cipherSpecs, String password) {
    	this.cipherSpecs = cipherSpecs;
    	this.cipherSession = new CipherSession(password);
    }    
    
    @Override
    public void init(Map<String, String> settings) throws Exception {
    	String password = settings.get(PROPERTY_PASSWORD);
    	String cipherSpecsListStr = settings.get(PROPERTY_CIPHER_SPECS);
    	
    	if (password == null || cipherSpecsListStr == null) {
    		throw new Exception("Settings '"+PROPERTY_CIPHER_SPECS+"' and '"+PROPERTY_PASSWORD+"' must both be filled.");
    	}
    	
    	initCipherSuites(cipherSpecsListStr);
    	initPassword(password);    	
    }
    
    private void initCipherSuites(String cipherSpecListStr) throws Exception {
    	String[] cipherSpecIdStrs = cipherSpecListStr.split(",");
    	
    	for (String cipherSpecIdStr : cipherSpecIdStrs) {
    		int cipherSpecId = Integer.parseInt(cipherSpecIdStr);
    		CipherSpec cipherSpec = CipherSpecs.getCipherSpec(cipherSpecId);
    		
    		if (cipherSpec == null) {
    			throw new Exception("Cannot find cipher suite with ID '"+cipherSpecId+"'");
    		}
    		
    		cipherSpecs.add(cipherSpec);
    	}
	}

	private void initPassword(String password) {
		cipherSession = new CipherSession(password);
	}

	@Override
	public OutputStream createOutputStream(OutputStream out) throws IOException {
    	return new MultiCipherOutputStream(out, cipherSpecs, cipherSession);    	
    }

    @Override
    public InputStream createInputStream(InputStream in) throws IOException {
    	return new MultiCipherInputStream(in, cipherSession);    	
    }    

    @Override
    public String toString() {
        return (nextTransformer == null) ? "Cipher" : "Cipher-"+nextTransformer;
    }     
}
