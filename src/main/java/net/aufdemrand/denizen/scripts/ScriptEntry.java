package net.aufdemrand.denizen.scripts;

import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.exceptions.ScriptEntryCreationException;
import net.aufdemrand.denizen.objects.*;
import net.aufdemrand.denizen.scripts.containers.ScriptContainer;
import net.aufdemrand.denizen.scripts.queues.ScriptQueue;
import net.aufdemrand.denizen.utilities.debugging.Debuggable;

import java.util.*;


/**
 * ScriptEntry contain information about a single entry from a dScript. It is used
 * by the CommandExecuter, among other parts of Denizen.
 *
 * @author Jeremy Schroeder
 * @version 1.0
 *
 */
public class ScriptEntry implements Cloneable, Debuggable {

    // The name of the command that will be executed
    private String command;

    // Command Arguments
    private List<String> args;
    private List<String> pre_tagged_args;

    // TimedQueue features
    private boolean instant = false;
    private boolean waitfor = false;

    // 'Attached' core context
    private dPlayer player = null;
    private dNPC npc = null;
    private dScript script = null;
    private ScriptQueue queue = null;

    // ScriptEntry Context
    private Map<String, Object> objects = new HashMap<String, Object>();


    // Allow cloning of the scriptEntry. Can be useful in 'loops', etc.
    @Override
    public ScriptEntry clone() throws CloneNotSupportedException {
        return (ScriptEntry) super.clone();
    }


    /**
     * Get a hot, fresh, script entry, ready for execution! Just supply a valid command,
     * some arguments, and bonus points for a script container (can be null)!
     *
     * @param command  the name of the command this entry will be handed to
     * @param arguments  an array of the arguments
     * @param script  optional ScriptContainer reference
     * @throws ScriptEntryCreationException  let's hope this never happens!
     */
    public ScriptEntry(String command, String[] arguments, ScriptContainer script) throws ScriptEntryCreationException {

        if (command == null)
            throw new ScriptEntryCreationException("dCommand 'name' cannot be null!");

        this.command = command.toUpperCase();

        // Knowing which script created this entry provides important context. We'll store
        // a dScript object of the container if script is not null.
        // Side note: When is script ever null? Think about situations such as the /ex command,
        // in which a single script entry is created and sent to be executed.
        if (script != null)
            this.script = script.getAsScriptArg();

        // Check if this is an 'instant' or 'waitfor' command. These are
        // features that are used with 'TimedScriptQueues'
        if (command.startsWith("^")) {
            instant = true;
            this.command = command.substring(1);
        } else if (command.startsWith("~")) {
            waitfor = true;
            this.command = command.substring(1);
        }

        // Store the args. The CommandExecuter will fill these out.
        this.args = new ArrayList<String>();
        if (arguments != null) {
            this.args = Arrays.asList(arguments);
            // Keep seperate list for 'un-tagged' copy of the arguments.
            // This will be useful if cloning the script entry for use in a loop, etc.
            this.pre_tagged_args = Arrays.asList(arguments);
        }

        // Check for replaceable tags. We'll try not to make a habit of checking for tags/doing
        // tag stuff if the script entry doesn't have any to begin with.
        for (String arg : args) {
            if (arg.contains("<") && arg.contains(">")) {
                has_tags = true;
                break;
            }
        }
    }


    // As explained, this is really just a micro-performance enhancer
    public boolean has_tags = false;


    /**
     * Adds a context object to the script entry. Just provide a key and an object.
     * Technically any type of object can be stored, however providing dObjects
     * is preferred.
     *
     * @param key  the name of the object
     * @param object  the object, preferably a dObject
     */
    public ScriptEntry addObject(String key, Object object) {
        if (object == null) return this;
        if (object instanceof dObject)
            ((dObject) object).setPrefix(key);
        objects.put(key.toUpperCase(), object);
        return this;
    }


    /**
     * If the scriptEntry lacks the object corresponding to the
     * key, set it to the first non-null argument
     *
     * @param key  The key of the object to check
     * @return  The scriptEntry
     */
    public ScriptEntry defaultObject(String key, Object... objects) throws InvalidArgumentsException {
        if (!this.objects.containsKey(key.toUpperCase()))
            for (Object obj : objects)
                if (obj != null) {
                    this.addObject(key, obj);
                    break;
                }

        // Check if the object has been filled. If not, throw new Invalid Arguments Exception.
        if (!hasObject(key)) throw new InvalidArgumentsException("Missing '" + key + "' argument!");
        else
            return this;
    }


    public List<String> getArguments() {
        return args;
    }



    ////////////
    // INSTANCE METHODS
    //////////

    /**
     * Gets the original, pre-tagged arguments, as constructed. This is simply a copy of
     * the original arguments, immune from any changes that may be made (such as tag filling)
     * by the CommandExecuter.
     *
     * @return  unmodified arguments from entry creation
     */
    public List<String> getOriginalArguments() {
        return pre_tagged_args;
    }


    public String getCommandName() {
        return command;
    }


    public ScriptEntry setArguments(List<String> arguments) {
        args = arguments;
        return this;
    }


    public void setCommandName(String commandName) {
        this.command = commandName;
    }



    //////////////////
    // SCRIPTENTRY CONTEXT
    //////////////

    public Map<String, Object> getObjects() {
        return objects;
    }


    public Object getObject(String key) {
        try {
            return objects.get(key.toUpperCase());
        } catch (Exception e) { return null; }
    }


    public dObject getdObject(String key) {
        try {
            // If an ENUM, return as an Element
            if (objects.get(key.toUpperCase()) instanceof Enum)
                return new Element(((Enum) objects.get(key.toUpperCase())).name());
            // Otherwise, just return the stored dObject
            return (dObject) objects.get(key.toUpperCase());
            // If not a dObject, return null
        } catch (Exception e) { return null; }
    }


    public Element getElement(String key) {
        try {
            return (Element) objects.get(key.toUpperCase());
        } catch (Exception e) { return null; }
    }


    public boolean hasObject(String key) {
        return (objects.containsKey(key.toUpperCase())
                && objects.get(key.toUpperCase()) != null);
    }



    /////////////
    // CORE LINKED OBJECTS
    ///////

    /**
     * Gets a dNPC reference to any linked NPC set by the CommandExecuter.
     *
     * @return
     */
    public dNPC getNPC() {
        return npc;
    }


    public boolean hasNPC() {
        return (npc != null);
    }


    public ScriptEntry setNPC(dNPC dNPC) {
        this.npc = dNPC;
        return this;
    }


    public dPlayer getPlayer() {
        return player;
    }


    public boolean hasPlayer() {
        return (player != null);
    }


    public ScriptEntry setPlayer(dPlayer player) {
        this.player = player;
        return this;
    }


    public dScript getScript() {
        return script;
    }


    public ScriptEntry setScript(String scriptName) {
        this.script = dScript.valueOf(scriptName);
        return this;
    }


    public ScriptQueue getResidingQueue() {
        return queue;
    }


    public void setSendingQueue(ScriptQueue scriptQueue) {
        queue = scriptQueue;
    }



    //////////////
    // TimedQueue FEATURES
    /////////

    public boolean isInstant() {
        return instant;
    }


    public ScriptEntry setInstant(boolean instant) {
        this.instant = instant;
        return this;
    }


    public boolean shouldWaitFor() {
        return waitfor;
    }


    public void setFinished(boolean finished) {
        waitfor = !finished;
    }



    ////////////
    // COMPATIBILITY
    //////////

    // Keep track of objects which were added by mass
    // so that IF can inject them into new entries.
    // This is ugly, but it will keep from breaking
    // previous versions of Denizen.
    public List<String> tracked_objects = new ArrayList<String>();
    public ScriptEntry trackObject(String key) {
        tracked_objects.add(key.toUpperCase());
        return this;
    }



    /////////////
    // DEBUGGABLE
    /////////

    @Override
    public boolean shouldDebug() throws Exception {
        return script.getContainer().shouldDebug();
    }

    @Override
    public boolean shouldFilter(String criteria) throws Exception {
        return script.getName().equalsIgnoreCase(criteria.replace("s@", ""));
    }

    public String reportObject(String id) {
        // If this script entry doesn't have the object being reported,
        // just return nothing.
        if (!hasObject(id))
            return "";

        // If the object is a dObject, there's a method for reporting them
        // in the proper format.
        if (getObject(id) instanceof dObject)
            return getdObject(id).debug();

        // If all else fails, fall back on the toString() method with the id of the
        // object being passed to aH.report(...)
        else return aH.debugObj(id, getObject(id));
    }
}
