/*
* Copyright (C) 2007-2008 Gilles Gigan (gilles.gigan@gmail.com)
* eResearch Centre, James Cook University (eresearch.jcu.edu.au)
*
* This program was developed as part of the ARCHER project
* (Australian Research Enabling Environment) funded by a   
* Systemic Infrastructure Initiative (SII) grant and supported by the Australian
* Department of Innovation, Industry, Science and Research
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public  License as published by the
* Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
* or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/
package au.edu.jcu.v4l4j.exceptions;

/**
 * Exceptions of this type are thrown whenever an error occurs retrieving or
 * setting the value of a control
 * 
 * @author gilles
 *
 */
public class ControlException extends V4L4JException {

	private static final long serialVersionUID = -8310718978974706151L;

	public ControlException(String message) {
		super(message);
	}

	public ControlException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public ControlException(Throwable throwable) {
		super(throwable);
	}

}
