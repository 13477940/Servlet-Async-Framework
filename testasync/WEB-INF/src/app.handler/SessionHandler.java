package app.handler;

import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;
import framework.web.session.service.SessionServiceStatic;

public class SessionHandler extends RequestHandler {

    private AsyncActionContext requestContext = null;

    @Override
    public void startup(AsyncActionContext asyncActionContext) {
        if(checkIsMyJob(asyncActionContext)) {
            this.requestContext = asyncActionContext;
            processRequest();
        } else {
            this.passToNext(asyncActionContext);
        }
    }

    @Override
    protected boolean checkIsMyJob(AsyncActionContext asyncActionContext) {
        return "session".equals(asyncActionContext.getParameters().get("act"));
    }

    private void processRequest() {
        String session_size = String.valueOf(SessionServiceStatic.getInstance().getUserMap().size());
        requestContext.printToResponse(session_size, new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                requestContext.complete();
            }
        });
    }

}
