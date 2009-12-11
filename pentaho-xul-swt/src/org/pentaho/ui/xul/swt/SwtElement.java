package org.pentaho.ui.xul.swt;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulContainer;
import org.pentaho.ui.xul.XulDomException;
import org.pentaho.ui.xul.containers.XulDeck;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.dom.Element;
import org.pentaho.ui.xul.impl.AbstractXulComponent;
import org.pentaho.ui.xul.swt.tags.SwtButton;
import org.pentaho.ui.xul.util.Orient;

public class SwtElement extends AbstractXulComponent {
  private static final long serialVersionUID = -4407080035694005764L;

  private static final Log logger = LogFactory.getLog(SwtElement.class);
  
  // Per XUL spec, STRETCH is the default align value.
  private SwtAlign align = SwtAlign.STRETCH;

  protected Orient orient = Orient.HORIZONTAL;

  private int flex = 0;

  protected PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

  private boolean disabled;

  public SwtElement(String tagName) {
    super(tagName);
  }
  
  @Override
  public void addChild(Element e) {
    super.addChild(e);

    if(e instanceof XulContainer){
      AbstractSwtXulContainer container = (AbstractSwtXulContainer) e;
      if(container.initialized == false){
        container.layout();
      }
    }

    if (initialized) {
      layout();
      ((XulComponent) e).onDomReady();
    }
  }
  
  public void addChildAt(Element c, int pos) {
    super.addChildAt(c, pos);
    if (initialized) {
      layout();
    }
  }

  @Override
  public void removeChild(Element ele) {
    super.removeChild(ele);
    if (ele instanceof XulComponent) {
      XulComponent comp = (XulComponent)ele;
      if (comp.getManagedObject() instanceof Widget) {
        Widget thisWidget = (Widget) comp.getManagedObject();
        if (thisWidget != null && !thisWidget.isDisposed()) {
          thisWidget.dispose();
        }
      }
    }
    
    if (initialized) {
      layout();
    }
  }
  

  public int getFlex() {
    return flex;
  }

  public void setFlex(int flex) {
    this.flex = flex;
  }


  public void setOrient(String orientation) {
    orient = Orient.valueOf(orientation.toUpperCase());
  }

  public String getOrient() {
    return orient.toString();
  }

  public Orient getOrientation() {
    return Orient.valueOf(getOrient());
  }

  @Override
  /**
   * This method attempts to follow the XUL rules
   * of layouting, using an SWT GridLayout. Any deviations 
   * from the general rules are applied in overrides to this method. 
   */
  public void layout() {
    super.layout();
    if (this instanceof XulDeck) {
      return;
    }

    if (!(getManagedObject() instanceof Composite)) {
      return;
    }

    Composite container = (Composite) getManagedObject();
    

    // Total all flex values.
    // If every child has a flex value, then the GridLayout's columns
    // should be of equal width. everyChildIsFlexing gives us that boolean. 

    int totalFlex = 0;
    int thisFlex = 0;
    boolean everyChildIsFlexing = true;

    for (Object child : this.getChildNodes()) {
      thisFlex = ((SwtElement) child).getFlex();
      if (thisFlex <= 0) {
        everyChildIsFlexing = false;
      }
      totalFlex += thisFlex;
    }

    // By adding the total flex "points" with the number
    // of child controls, we get a close relative size, using
    // columns in the GridLayout. 

    switch (orient) {
      case HORIZONTAL:
        int columnCount = this.getChildNodes().size() + totalFlex;
        GridLayout layout = new GridLayout(columnCount, everyChildIsFlexing);
        if(this.getPadding() > -1){
          layout.marginWidth = this.getPadding(); 
          layout.marginHeight = this.getPadding();
        }
        if(this.getSpacing() > -1){
          layout.horizontalSpacing = this.getSpacing();
          layout.verticalSpacing = this.getSpacing();
        }
        container.setLayout(layout);
        
        break;
      case VERTICAL:
        layout = new GridLayout();
        if(this.getPadding() > -1){
          layout.marginWidth = this.getPadding(); 
          layout.marginHeight = this.getPadding();
        }
        if(this.getSpacing() > -1){
          layout.horizontalSpacing = this.getSpacing();
          layout.verticalSpacing = this.getSpacing();
        }
        container.setLayout(layout);
        break;
    }

    for (Object child : this.getChildNodes()) {

      SwtElement swtChild = (SwtElement) child;

      // some children have no object they are managing... skip these kids!

      Object mo = swtChild.getManagedObject();
      if (mo == null || !(mo instanceof Control) || swtChild instanceof XulDialog){
        continue;
      }

      Control c = (Control) swtChild.getManagedObject();

      GridData data = new GridData();

      // How many columns or rows should the control span? Use the flex value plus
      // 1 "point" for the child itself. 
      data.horizontalSpan = orient.equals(Orient.HORIZONTAL) ? swtChild.getFlex() + 1 : 1;
      data.verticalSpan = orient.equals(Orient.VERTICAL) ? swtChild.getFlex() + 1 : 1;

      // In XUL, flex defines how the children grab the excess space 
      // in the container - therefore, we need to grab the excess space... 

      
      switch (orient) {
        case HORIZONTAL:
          data.verticalAlignment = SWT.FILL;
          data.grabExcessVerticalSpace = true;
          break;
        case VERTICAL:
          data.horizontalAlignment = SWT.FILL;
          data.grabExcessHorizontalSpace = true;
          break;
      }
      
      
      if (swtChild.getFlex() > 0) {
        if(swtChild.getWidth() == 0){
          data.grabExcessHorizontalSpace = true;
          data.horizontalAlignment = SWT.FILL;
        }

        if(swtChild.getHeight() == 0){
          data.grabExcessVerticalSpace = true;
          data.verticalAlignment = SWT.FILL;
        }
      }

      if(swtChild.getWidth() > 0){
        data.widthHint = swtChild.getWidth();
      }

      if(swtChild.getHeight() > 0){
        data.heightHint = swtChild.getHeight();
      }
      
      // And finally, deal with the align attribute...
      // Align is the PARENT'S attribute, and affects the 
      // opposite direction of the orientation.

      if (((XulComponent) swtChild).getAlign() != null) {
        SwtAlign swtAlign = SwtAlign.valueOf(((XulContainer) swtChild).getAlign().toString());
        
        if (orient.equals(Orient.HORIZONTAL)) {

          if(swtChild.getHeight() < 0){
            data.grabExcessVerticalSpace = true;
          }
          
        } else { //Orient.VERTICAL

          if(swtChild.getWidth() < 0){
            data.grabExcessHorizontalSpace = true;
          }
        }
      }
      c.setLayoutData(data);
    }
    container.layout(true);
  }

  @Override
  /**
   * Important to understand that when using this method in the 
   * SWT implementation:
   * 
   * SWT adds new children positionally based on add order. This
   * means that a child that was added third in a list of 5 can't be 
   * "replaced" to its third position in the dialog, it can only 
   * be added to the end of the child list. 
   * 
   * Major SWT limitation. Replacement can only be used in 
   * a limited number of cases. 
   */
  public void replaceChild(XulComponent oldElement, XulComponent newElement) throws XulDomException {

    super.replaceChild(oldElement, newElement);
    Widget thisWidget = (Widget) oldElement.getManagedObject();
    if (!thisWidget.isDisposed()) {
      thisWidget.dispose();
    }
    ((Control) newElement.getManagedObject()).setParent((Composite) this.getManagedObject());

    layout();
  }

  public void setOnblur(String method) {
    throw new NotImplementedException();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changeSupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changeSupport.removePropertyChangeListener(listener);
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled= disabled;
  }

  public void adoptAttributes(XulComponent component) {
    throw new NotImplementedException();
  }
  
  public void setMenu(final Menu menu){
    //the generic impl... override if you need a more sophisticated handling of the menu
    if(getManagedObject() instanceof Control){
      final Control c = (Control) getManagedObject();
      c.addMouseListener(new MouseAdapter(){

        @Override
        public void mouseDown(MouseEvent evt) {
          Control source = (Control) evt.getSource();
          Point pt = source.getDisplay().map(source, null, new Point(evt.x, evt.y));
          menu.setLocation(pt.x, pt.y);
          menu.setVisible(true);
        }
        
      });
    }
  }
  
  public void setPopup(Menu menu){
    //the generic impl... override if you need a more sophisticated handling of the popupmenu
    if(getManagedObject() instanceof Control){
      ((Control) getManagedObject()).setMenu(menu);
    }
  }

  @Override
  public void onDomReady() {
    super.onDomReady();
    if(this.context != null){
      XulComponent pop = this.getDocument().getElementById(context);
      if(pop == null){
        logger.error("could not find popup menu ("+context+") to add to this component");
      } else {
        setPopup((Menu) pop.getManagedObject());
      }
    }
    

    if(this.menu != null){
      XulComponent pop = this.getDocument().getElementById(menu);
      if(pop == null){
        logger.error("could not find popup menu ("+context+") to add to this component");
      } else {
        setMenu((Menu) pop.getManagedObject());
      }
    }
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if(getManagedObject() instanceof Control){
      ((Control) getManagedObject()).setVisible(visible);
    }
  }
  
  

}
