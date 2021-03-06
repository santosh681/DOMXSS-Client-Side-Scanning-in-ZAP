/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Copyright 2014 The ZAP Development Team
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.ascanrulesAlpha;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.log4j.Logger;
import org.parosproxy.paros.core.scanner.AbstractAppPlugin;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.core.scanner.Category;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.Constant;

import java.io.IOException;

/**
 * Active scan rule which checks whether or not HTTPS content is also available via HTTP 
 * https://code.google.com/p/zaproxy/issues/detail?id=174
 * @author kingthorin+owaspzap@gmail.com
 */
public class HttpsAsHttpScanner extends AbstractAppPlugin {

	/**
	 * Prefix for internationalised messages used by this rule
	 */
	private static final String MESSAGE_PREFIX = "ascanalpha.httpsashttpscanner.";
	private static final int PLUGIN_ID = 10047;

	private static Logger log = Logger.getLogger(HttpsAsHttpScanner.class);
	
	@Override
	public int getId() {
		return PLUGIN_ID;
	}

	@Override
	public String getName() {
		return Constant.messages.getString(MESSAGE_PREFIX + "name");
	}
	
	public String getDescription() {
		return Constant.messages.getString(MESSAGE_PREFIX + "desc");
	}

	public String getSolution() {
		return Constant.messages.getString(MESSAGE_PREFIX + "soln");
	}

	public String getReference() {
		return Constant.messages.getString(MESSAGE_PREFIX + "refs");
	}

	@Override
	public String[] getDependency() {
		return null;
	}

	@Override
	public int getCategory() {
		return Category.MISC;
	}
	
	@Override
	public int getRisk() {
		return Alert.RISK_LOW;
	}

	@Override
	public int getCweId() {
		return 0; // The CWE id
	}

	@Override
	public int getWascId() {
		return 0; // The WASC ID
	}

	@Override
	public void init() {

	}

	@Override
	public void scan() {
		
		if (this.isStop()) { // Check if the user stopped things
			if (log.isDebugEnabled()) {
				log.debug("Scanner "+this.getName()+" Stopping.");
			}
			return; // Stop!
		}
		
		if (!this.getBaseMsg().getRequestHeader().isSecure()) { //Base request isn't HTTPS
			if (log.isDebugEnabled())  log.debug ("The original request was not HTTPS, so there is not much point in looking further.");
			return;	
		}
		
		int originalStatusCode = this.getBaseMsg().getResponseHeader().getStatusCode();
		if (originalStatusCode == HttpStatus.SC_NOT_FOUND || originalStatusCode == 0) {
			if (log.isDebugEnabled())  log.debug ("The original request was not successfuly completed (status = "+ originalStatusCode +"), so there is not much point in looking further.");
			return;				
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Checking if " + this.getBaseMsg().getRequestHeader().getURI() + " is available via HTTP.");
		}
		
		HttpMessage newRequest = doRequest();
		
		int newStatusCode = newRequest.getResponseHeader().getStatusCode();
		
		if (newStatusCode == HttpStatus.SC_OK) { // 200 Success

			URI uri = newRequest.getRequestHeader().getURI();
			
			try {
				bingo(this.getRisk(), //Risk
						Alert.WARNING, //Confidence/Reliability
						this.getName(), //Name
						this.getDescription(), //Description
						uri.toString(), //URI
						null, //Param
						"", //Attack
						uri.toString(), //OtherInfo 
						this.getSolution(), //Solution
						uri.toString(), //Evidence
						this.getCweId(), //CWE ID
						this.getWascId(), //WASC ID
						newRequest); //HTTPMessage
			} catch (Exception e) {
				log.error("Error raising alert" + e.getMessage(), e);
			}
		}
	}
	
	private HttpMessage doRequest() {
		
		HttpMessage newRequest = this.getNewMsg();
				
		try{
		URI newURI = new URI("http",
			newRequest.getRequestHeader().getURI().getUserinfo(),
			newRequest.getRequestHeader().getURI().getHost(),
			newRequest.getRequestHeader().getURI().getPort(),
			newRequest.getRequestHeader().getURI().getPath(),
			newRequest.getRequestHeader().getURI().getPathQuery(),
			newRequest.getRequestHeader().getURI().getFragment());
		
			newRequest.getRequestHeader().setURI(newURI); //Set the URI based on the newly built URL
		} catch (URIException e)
		{
			log.error("Error creating HTTP URL from HTTPS URL.");
			log.error(e.getMessage(), e);
		}

		newRequest.setCookieParams(this.getBaseMsg().getCookieParams());
		try {
			sendAndReceive(newRequest, false);
		} catch (IOException e) {
			log.error("Error scanning a request via HTTP when the original was HTTPS.");
		}
		return(newRequest);
	}
}
