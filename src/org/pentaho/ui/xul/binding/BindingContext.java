package org.pentaho.ui.xul.binding;

import java.beans.Expression;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulEventSource;

public class BindingContext {
  
  private XulDomContainer container;
  private List<Binding> bindings = new ArrayList<Binding>();
  
  public BindingContext(XulDomContainer container){
    this.container = container;
  }
  
  public void add(XulComponent source, String expr){
    BindingExpression expression = BindingExpression.parse(expr);
    XulComponent target = container.getDocumentRoot().getElementById(expression.target);
    Binding newBinding = new Binding(source, expression.sourceAttr, target, expression.targetAttr);
    add(newBinding);
  }
  
  public void add(Binding bind){
    bindings.add(bind); 
    //forward binding
    setupBinding(bind.getSource(), bind.getSourceAttr(), bind.getTarget(), bind.getTargetAttr());
    
    //reverse binding
    setupBinding(bind.getTarget(), bind.getTargetAttr(), bind.getSource(), bind.getSourceAttr());
  }
  

  private void setupBinding(final XulEventSource source, final String sourceAttr, final XulEventSource target, final String targetAttr){

    Method method = null;
    String setMethodName = "set"+(String.valueOf(targetAttr.charAt(0)).toUpperCase())+targetAttr.substring(1);
    try{
    
      //Get value from one in order to determine type of the attribute
      String methodName = "get"+(String.valueOf(targetAttr.charAt(0)).toUpperCase())+targetAttr.substring(1);
      Expression state = new Expression(target, methodName, null);
      Object val = state.getValue();
      Class clazz = val.getClass();
      
      try{
        method = target.getClass().getMethod(setMethodName, new Class[]{clazz});
      } catch(NoSuchMethodException e){
        //Could not find an appropriate setter. Conversion may be required.
        //TODO: handle type conversions? Maybe?
        throw new XulBindingException(e);
      }
      
      
    } catch(NoSuchMethodException e){
      try{
        //Hum. method not found. Check is[fieldName] boolean convention
        String methodName = "is"+(String.valueOf(targetAttr.charAt(0)).toUpperCase())+targetAttr.substring(1);
        Expression state = new Expression(target, methodName, null);
        //if you made it here you were correct that it IS a boolean
        
        //change args to primimtive boolean type instead of Boolean Class
        method = target.getClass().getMethod(setMethodName, new Class[]{Boolean.TYPE} );
        
      } catch(Exception ex){
        System.out.println("Set method not found: "+ex.getMessage());
        ex.printStackTrace(System.out);
      }
      
    } catch(Exception e){
      System.out.println("Unknown error finding binding method: "+e.getMessage());
      e.printStackTrace(System.out);
    }
    
    //setup prop change listener to handle binding
    final Method finalMethod = method;
    PropertyChangeListener listener = new PropertyChangeListener(){
      public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("In PropChange Listener");
        if(evt.getPropertyName().equalsIgnoreCase(sourceAttr)){
          try{
            finalMethod.invoke(target, evt.getNewValue());
          } catch(Exception e){
            System.out.println("Error invoking binding method: "+e.getMessage());
            e.printStackTrace(System.out);
          }
        }
      }
    };
    
    source.addPropertyChangeListener(listener);
  }
  
}