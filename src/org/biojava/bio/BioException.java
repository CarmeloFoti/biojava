/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 */

package org.biojava.bio;

import org.biojava.utils.NestedException;

/**
 * A nestable biological exception.
 *
 * @author Matthew Pocock
 *
 * @for.user
 * Catch this whenever a method throws it, and you want to handle the exception.
 *
 * @for.user
 * Throw this whenever you have caught a Throwable and need to throw an
 * Exception or BioException in your method.
 *
 * @for.powerUser
 * Be sure to wrap up any causual throwable. It makes debugging your (and
 * other peoples') code much easier.
 */
public class BioException extends NestedException {
  /**
   * Create a new BioException with a message.
   *
   * @param message  the message
   */
  public BioException(String message) {
	  super(message);
  }

  /**
   * Create a new BioException with a cause.
   *
   * @param ex  the Throwable that caused this BioException
   */
  public BioException(Throwable ex) {
    super(ex);
  }

  /**
   * Create a new BioException with a cause and a message.
   *
   * @param ex  the Throwable that caused this BioException
   * @param message  the message
   */
  public BioException(Throwable ex, String message) {
    super(ex, message);
  }
  
  /**
   * Create a new BioException.
   */
  public BioException() {
	  super();
  }
}
