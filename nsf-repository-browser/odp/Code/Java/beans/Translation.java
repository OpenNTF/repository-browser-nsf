/**
 * Copyright (c) 2018 Christian Guedemann, Jesse Gallagher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package beans;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;

import com.ibm.xsp.application.ApplicationEx;
import com.ibm.xsp.designer.context.XSPContext;
import com.ibm.xsp.extlib.util.ExtLibUtil;
import com.ibm.xsp.model.DataObject;

/**
 * Provides access to translation strings from the "translation" bundle
 * on the classpath.
 * 
 * @author Jesse Gallagher
 * @since 1.1
 */
public class Translation implements Serializable, DataObject {
	private static final long serialVersionUID = 1L;
	
	public static final String BEAN_NAME = "translation"; //$NON-NLS-1$

	private transient ResourceBundle bundle_;
	private Map<Object, String> cache_ = new HashMap<>();

	public static Translation get() {
		Translation existing = (Translation)ExtLibUtil.resolveVariable(BEAN_NAME);
		return existing == null ? new Translation() : existing;
	}
	
	public static String translate(String key, Object... params) {
		Translation translation = get();
		String translated = translation.getValue(key);
		return MessageFormat.format(translated, params);
	}

	@Override
	public Class<String> getType(Object key) {
		return String.class;
	}

	@Override
	public String getValue(Object key) {
		if(!cache_.containsKey(key)) {
			try {
				ResourceBundle bundle = getTranslationBundle();
				cache_.put(key, bundle.getString(String.valueOf(key)));
			} catch(IOException ioe) {
				throw new RuntimeException(ioe);
			} catch(MissingResourceException mre) {
				cache_.put(key, "[Untranslated " + key + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return cache_.get(key);
	}

	@Override
	public boolean isReadOnly(Object key) {
		return true;
	}

	@Override
	public void setValue(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	// *******************************************************************************
	// * Internal utility methods
	// *******************************************************************************

	private ResourceBundle getTranslationBundle() throws IOException {
		if(bundle_ == null) {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			ApplicationEx app = (ApplicationEx)facesContext.getApplication();
			bundle_ = app.getResourceBundle("translation", XSPContext.getXSPContext(facesContext).getLocale()); //$NON-NLS-1$
		}
		return bundle_;
	}
}
