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

import java.util.*;
import java.io.*;

import org.biojava.utils.*;

/**
 * Annotation implementation which allows new key-value
 * pairs to be layered on top of an underlying Annotation.
 * When <code>getProperty</code> is called, we first check
 * for a value stored in the overlay.  If this fails, the
 * underlying <code>Annotation</code> is checked.  Values
 * passed to <code>setProperty</code> are always stored
 * within the overlay.
 * <P>
 * So that multiple values may be assigned to the same key, for example,
 * multiple "/note" fields on a GenBank feature, if a value would be
 * overwritten, a list will instead be composed of the old value and the new
 * one.  Therefore, use caution when adding a list as a value.
 *
 * @author Thomas Down
 * @author Matthew Pocock
 * @author Greg Cox
 */

public class OverlayAnnotation implements Annotation, Serializable {
  /**
   * The object to do the hard work of informing others of changes.
   */
  protected transient ChangeSupport changeSupport = null;
  private transient ChangeListener propertyForwarder = null;

  private Annotation parent;
  private Map overlay = null;

  protected void generateChangeSupport(ChangeType changeType) {
    if(changeSupport == null) {
      changeSupport = new ChangeSupport();
    }

    if(
      ((changeType == null) || (changeType == Annotation.PROPERTY)) &&
      (propertyForwarder == null)
    ) {
      propertyForwarder = new PropertyForwarder(
        OverlayAnnotation.this,
        changeSupport
      );
      parent.addChangeListener(
        propertyForwarder,
        Annotation.PROPERTY
      );
    }
  }

    protected Map getOverlay() {
	if (overlay == null)
	    overlay = new HashMap();
	return overlay;
    }

  /**
   * Construct an annotation which can overlay new key-value
   * pairs onto an underlying annotation.
   *
   * @param par The `parent' annotation, on which new
   *            key-value pairs can be layered.
   */

  public OverlayAnnotation(Annotation par) {
    parent = par;
  }

  public void setProperty(Object key, Object value)
  throws ChangeVetoException {
	// This mess is to handle cases where there is already a value associated
	// with the key.
	Object newValue;
	Object oldValue = null;
	try
	{
		if(this.getOverlay().containsKey(key))
		{
			oldValue = this.getProperty(key);
			// If the old value is already a list, append the new value to it.
			if (oldValue instanceof List)
			{
				newValue = oldValue;
				((List)newValue).add(value);
			}
			// Otherwise, construct a new list with the old and new values.
			else
			{
				newValue = new ArrayList();
				((List)newValue).add(oldValue);
				((List)newValue).add(value);
			}
		}
		else
		{
			newValue = value;
		}
	}
	// Catches ClassCast, Unsupported operation, and Illegal argument exceptions
	// from newValue.add.  If this operation fails, create a new list with the
	// new and old values.
	catch (RuntimeException re)
	{
		newValue = new ArrayList();
		((List)newValue).add(oldValue);
		((List)newValue).add(value);
	}

    if(changeSupport == null) {
      getOverlay().put(key, newValue);
    } else {
      ChangeEvent ce = new ChangeEvent(
        this,
        Annotation.PROPERTY,
        new Object[] {key, newValue},
        new Object[] {key, getProperty(key)}
      );
      synchronized(changeSupport) {
        changeSupport.firePreChangeEvent(ce);
        getOverlay().put(key, newValue);
        changeSupport.firePostChangeEvent(ce);
      }
    }
  }

  public Object getProperty(Object key) {
      Object val = null;
      if (overlay != null)
	  val = overlay.get(key);
      if (val != null) {
	  return val;
      }
      return parent.getProperty(key);
  }

  public boolean containsProperty(Object key) {
     if(
       (overlay != null) &&
       (overlay.containsKey(key))
     ) {
       return true;
     } else {
       return parent.containsProperty(key);
     }
   }

    private Object getPropertySilent(Object key) {
	try {
	    return getProperty(key);
	} catch (NoSuchElementException ex) {
	    return null;
	}
    }

  /**
   * Return a <code>Set</code> containing all key objects
   * visible in this annotation.  The <code>Set</code> is
   * unmodifiable, but will dynamically reflect changes made
   * to the annotation.
   */
  public Set keys() {
    return new OAKeySet();
  }

  /**
   * Return a <code>Map</code> view onto this annotation.
   * The returned <code>Map</code> is unmodifiable, but will
   * dynamically reflect any changes made to this annotation.
   */

  public Map asMap() {
    return new OAMap();
  }

  public void addChangeListener(ChangeListener cl) {
    generateChangeSupport(null);

    synchronized(changeSupport) {
      changeSupport.addChangeListener(cl);
    }
  }

  public void addChangeListener(ChangeListener cl, ChangeType ct) {
    generateChangeSupport(ct);

    synchronized(changeSupport) {
      changeSupport.addChangeListener(cl, ct);
    }
  }

  public void removeChangeListener(ChangeListener cl) {
    if(changeSupport != null) {
      synchronized(changeSupport) {
        changeSupport.removeChangeListener(cl);
      }
    }
  }

  public void removeChangeListener(ChangeListener cl, ChangeType ct) {
    if(changeSupport != null) {
      synchronized(changeSupport) {
        changeSupport.removeChangeListener(cl, ct);
      }
    }
  }

  private class OAKeySet extends AbstractSet {
    private Set parentKeys;

     private OAKeySet() {
       super();
       parentKeys = parent.keys();
     }

     public Iterator iterator() {
       return new Iterator() {
         Iterator oi = (overlay != null) ? overlay.keySet().iterator()
	                                 : Collections.EMPTY_SET.iterator();
         Iterator pi = parentKeys.iterator();
         Object peek = null;

         public boolean hasNext() {
           if (peek == null)
             peek = nextObject();
             return (peek != null);
         }

         public Object next() {
           if (peek == null) {
             peek = nextObject();
           }
           if (peek == null) {
             throw new NoSuchElementException();
           }
           Object o = peek;
           peek = null;
           return o;
         }

         private Object nextObject() {
           if (oi.hasNext()) {
             return oi.next();
           }
           Object po = null;
           while (po == null && pi.hasNext()) {
             po = pi.next();
             if (overlay != null && overlay.containsKey(po)) {
               po = null;
             }
           }
           return po;
         }

         public void remove() {
           throw new UnsupportedOperationException();
         }
       };
     }

     public int size() {
       int i = 0;
       Iterator keys = iterator();
       while(keys.hasNext()) {
         keys.next();
         ++i;
       }
       return i;
     }

     public boolean contains(Object o) {
       return (overlay != null && overlay.containsKey(o)) || parentKeys.contains(o);
     }
  }

  private class OAEntrySet extends AbstractSet {
    OAKeySet ks;

    private OAEntrySet() {
	    super();
	    ks = new OAKeySet();
    }

    public Iterator iterator() {
	    return new Iterator() {
        Iterator ksi = ks.iterator();

        public boolean hasNext() {
          return ksi.hasNext();
        }

        public Object next() {
          Object k = ksi.next();
          Object v = getProperty(k);
          return new OAMapEntry(k, v);
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
	    };
    }

    public int size() {
	    return ks.size();
    }
  }

  private class OAMapEntry implements Map.Entry {
    private Object key;
    private Object value;

    private OAMapEntry(Object key, Object value) {
	    this.key = key;
	    this.value = value;
    }

    public Object getKey() {
	    return key;
    }

    public Object getValue() {
	    return value;
    }

    public Object setValue(Object v) {
	    throw new UnsupportedOperationException();
    }

    public boolean equals(Object o) {
	    if (! (o instanceof Map.Entry)) {
        return false;
      }

	    Map.Entry mo = (Map.Entry) o;
	    return ((key == null ? mo.getKey() == null : key.equals(mo.getKey())) &&
		    (value == null ? mo.getValue() == null : value.equals(mo.getValue())));
    }

    public int hashCode() {
	    return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }
  }

  private class OAMap extends AbstractMap {
    OAEntrySet es;
    OAKeySet ks;

    private OAMap() {
	    super();
	    ks = new OAKeySet();
	    es = new OAEntrySet();
    }

    public Set entrySet() {
	    return es;
    }

    public Set keySet() {
	    return ks;
    }

	  public Object get(Object key) {
	    try {
        return getProperty(key);
	    } catch (NoSuchElementException ex) {
	    }

	    return null;
    }
  }

  protected class PropertyForwarder extends ChangeForwarder {
    public PropertyForwarder(Object source, ChangeSupport cs) {
      super(source, cs);
    }

    public ChangeEvent generateEvent(ChangeEvent ce) {
      ChangeType ct = ce.getType();
      if(ct == Annotation.PROPERTY) {
        Object curVal = ce.getChange();
        if(curVal instanceof Object[]) {
          Object[] cur = (Object []) curVal;
          if(cur.length == 2) {
            Object key = cur[0];
            Object value = cur[0];
            if(getProperty(key) != value) {
              return new ChangeEvent(
                getSource(),
                Annotation.PROPERTY,
                curVal,
                ce.getPrevious(),
                ce
              );
            }
          }
        }
      }
      return null;
    }
  }
}
