package net.aufdemrand.denizen.scripts.commands.player;

import java.util.Arrays;
import java.util.List;

import net.aufdemrand.denizen.objects.*;

import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.scripts.ScriptRegistry;
import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
import net.aufdemrand.denizen.scripts.containers.core.FormatScriptContainer;
import net.aufdemrand.denizen.utilities.debugging.dB;

/**
 * Sends a message to Players.
 *
 * @author Jeremy Schroeder
 * @version 1.0
 */

public class NarrateCommand extends AbstractCommand {

    public static final    String  FORMAT_ARG = "format, f";
    public static final    String  TARGET_ARG = "target, targets, t";

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {


        if (scriptEntry.getArguments().size() > 4)
            throw new InvalidArgumentsException("Too many arguments! Did you forget a 'quote'?");

        // Iterate through arguments
        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {
            if (!scriptEntry.hasObject("format") && arg.matchesPrefix(FORMAT_ARG)) {
                FormatScriptContainer format = null;
                String formatStr = arg.getValue();
                format = ScriptRegistry.getScriptContainerAs(formatStr, FormatScriptContainer.class);
                if (format == null) dB.echoError("Could not find format script matching '" + formatStr + '\'');
                scriptEntry.addObject("format", format);
            }

            // Add players to target list
            else if ((arg.matchesPrefix("target") || arg.matchesPrefix(TARGET_ARG))) {
                scriptEntry.addObject("targets", ((dList)arg.asType(dList.class)).filter(dPlayer.class));
            }

            // Use raw_value as to not accidentally strip a value before any :'s.
            else {
                if (!scriptEntry.hasObject("text"))
                    scriptEntry.addObject("text", new Element(arg.raw_value));
            }
        }

        // If there are no targets, check if you can add this player
        // to the targets
        if (!scriptEntry.hasObject("targets"))
            scriptEntry.addObject("targets",
                    (scriptEntry.hasPlayer() ? Arrays.asList(scriptEntry.getPlayer()) : null));

        if (!scriptEntry.hasObject("text"))
            throw new InvalidArgumentsException("Missing any text!");

    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {
        // Get objects
        List<dPlayer> targets = (List<dPlayer>) scriptEntry.getObject("targets");
        String text = scriptEntry.getElement("text").asString();
        FormatScriptContainer format = (FormatScriptContainer) scriptEntry.getObject("format");

        // Report to dB
        dB.report(scriptEntry, getName(),
                aH.debugObj("Narrating", text)
                        + aH.debugObj("Targets", targets)
                        + (format != null ? aH.debugObj("Format", format.getName()) : ""));

        if (targets == null) {
            return;
        }
        for (dPlayer player : targets) {
            if (player != null && player.isOnline())
                player.getPlayerEntity().sendMessage(format != null ? format.getFormattedText(scriptEntry) : text);
            else
                dB.echoError("Narrated to non-existent or offline player!");
        }
    }

}
