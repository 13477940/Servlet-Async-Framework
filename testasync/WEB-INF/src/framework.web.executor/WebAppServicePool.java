package framework.web.executor;

import framework.web.handler.RequestHandler;

import java.util.ArrayList;

public abstract class WebAppServicePool {

    private ArrayList<RequestHandler> handlers;

    public WebAppServicePool() {
        handlers = new ArrayList<>();
    }

    public void addHandler(RequestHandler handler) {
        handlers.add(handler);
    }

    public RequestHandler getHandler(int index) {
        return handlers.get(index);
    }

    public ArrayList<RequestHandler> prototype() {
        return handlers;
    }

}
