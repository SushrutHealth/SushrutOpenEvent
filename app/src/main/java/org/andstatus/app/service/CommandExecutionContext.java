package org.andstatus.app.service;

import android.content.Context;

import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.MyContext;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.data.TimelineType;
import org.andstatus.app.util.MyLog;

import java.util.LinkedList;

public class CommandExecutionContext {
    private CommandData commandData;
    private LinkedList<CommandData> stackOfCommandDataOfExecSteps = new LinkedList<CommandData>();

    private MyAccount ma;
    private TimelineType timelineType;    
    /**
     * The Timeline (if any) is of this User 
     */
    private long timelineUserId = 0;

    private MyContext myContext;

    public CommandExecutionContext(CommandData commandData, MyAccount ma) {
        this(MyContextHolder.get(), commandData, ma);
    }

    public CommandExecutionContext(MyContext myContext, CommandData commandData, MyAccount ma) {
        if (commandData == null) {
            throw new IllegalArgumentException( "CommandData is null");
        }
        this.commandData = commandData;
        this.ma = ma;
        this.timelineType = commandData.getTimelineType();
        this.myContext = myContext;
    }

    public MyAccount getMyAccount() {
        return ma;
    }
    public void setMyAccount(MyAccount ma) {
        this.ma = ma;
    }

    public MyContext getMyContext() {
        return myContext;
    }

    public Context getContext() {
        return myContext.context();
    }

    public TimelineType getTimelineType() {
        return timelineType;
    }
    public CommandExecutionContext setTimelineType(TimelineType timelineType) {
        this.timelineType = timelineType;
        return this;
    }

    public long getTimelineUserId() {
        return timelineUserId;
    }

    public CommandExecutionContext setTimelineUserId(long timelineUserId) {
        this.timelineUserId = timelineUserId;
        return this;
    }

    public CommandData getCommandData() {
        return commandData;
    }

    void onOneExecStepLaunch() {
        stackOfCommandDataOfExecSteps.addFirst(commandData);
        commandData = CommandData.forOneExecStep(this);
    }

    void onOneExecStepEnd() {
        CommandData storedBeforeExecStep = stackOfCommandDataOfExecSteps.removeFirst();
        storedBeforeExecStep.accumulateOneStep(commandData);
        commandData = storedBeforeExecStep;
    }

    public CommandResult getResult() {
        return commandData.getResult();
    }

    @Override
    public String toString() {
        return MyLog.formatKeyValue(
                "CommandExecutionContext",
                (ma == null ? "" : ma.toString() + ",")
                + (TimelineType.UNKNOWN.equals(timelineType) ? "" : timelineType
                        .toString() + ",")
                        + (timelineUserId == 0 ? "" : "userId:" + timelineUserId + ",")
                        + commandData.toString());
    }

    public String toExceptionContext() {
        StringBuilder builder = new StringBuilder(100);
        if (ma != null) {
            builder.append(ma.getAccountName() + ", ");
        }
        if (!TimelineType.UNKNOWN.equals(timelineType)) {
            builder.append(timelineType.toString() + ", ");
        }
        if (timelineUserId != 0) {
            builder.append("userId:" + timelineUserId + ", ");
        }
        return builder.toString();
    }
}
