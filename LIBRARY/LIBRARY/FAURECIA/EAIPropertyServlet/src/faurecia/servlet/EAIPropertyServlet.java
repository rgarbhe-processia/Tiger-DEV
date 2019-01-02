package faurecia.servlet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.jdl.MatrixSession;

import fpdm.eai.ServletRequest;
import matrix.db.Context;
import matrix.util.MatrixException;

@WebServlet("/EAIPropertyServlet")
public class EAIPropertyServlet extends HttpServlet {
    private static final long serialVersionUID = -1103446156909051556L;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        System.out.println("EAIPropertyServlet");
        ServletRequest sr = null;
        MatrixSession ms = null;
        ObjectInputStream in = new ObjectInputStream(request.getInputStream());
        try {
            sr = (ServletRequest) in.readObject();
            ms = sr.session;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        in.close();

        Context context;
        try {
            context = new Context(ms);

            System.out.println(sr.requestName);
            System.out.println(sr.params);

            List<Object> parameters = new ArrayList<Object>();
            parameters.add(context);
            parameters.addAll(sr.params);

            List<Class> argumentsListClasses = new ArrayList<Class>();
            //parameters.stream().forEach(item -> argumentsListClasses.add(item.getClass()));
            
            for (Object parameter : parameters) {
                argumentsListClasses.add(parameter.getClass());
            }

            System.out.println(argumentsListClasses);
                        
            //Class[] argumentsClasses = argumentsListClasses.stream().toArray(Class[]::new);
            
            Class[] argumentsClasses = new Class[argumentsListClasses.size()];
            
            for (int x=0; x<argumentsListClasses.size(); x++) {
                argumentsClasses[x] = argumentsListClasses.get(x); 
            }
            System.out.println(argumentsClasses);
            
            try {
                Method method = EnoviaResourceBundle.class.getMethod(sr.requestName, argumentsClasses);
                //Object bleh = method.invoke(null, parameters.stream().toArray(Object[]::new));
                
                Object[] params = new Object[parameters.size()];
                
                for (int x=0; x<parameters.size(); x++) {
                    params[x] = parameters.get(x);
                }
                
                Object bleh = method.invoke(null, params);
                System.out.println("Object returned : ");
                System.out.println(bleh);
                ObjectOutputStream oos = new ObjectOutputStream(response.getOutputStream());
                oos.writeObject(bleh);
                oos.close();
            } catch (NoSuchMethodException e2) {
                e2.printStackTrace();
            } catch (SecurityException e2) {
                e2.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            try {
                context.connect();
                System.out.println(context.getUser());
            } catch (MatrixException e) {
                e.printStackTrace();
            }

            System.out.println(context.getUser());

        } catch (MatrixException e1) {
            e1.printStackTrace();
        }
    }
}
