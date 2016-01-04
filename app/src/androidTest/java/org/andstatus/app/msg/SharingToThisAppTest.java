package org.andstatus.app.msg;

import android.test.InstrumentationTestCase;

import org.andstatus.app.msg.TimelineActivity;

public class SharingToThisAppTest extends InstrumentationTestCase {
    public void testInputSharedContent() {
        String part1 = "This is a long a long post";
        String text = part1 + " that doesn't fit into subject";
        String prefix = "Message - ";
        String ellipsis = "…";
        oneInputSharedContent(prefix, part1, text, ellipsis, false);
        oneInputSharedContent(prefix, "Another text for a subject", text, ellipsis, true);
        oneInputSharedContent("", "This is a long but not exact subject", text, "", true);
        oneInputSharedContent("Tweet:", part1, text, ellipsis, false);
        oneInputSharedContent(prefix, part1, text, "", false);
        oneInputSharedContent(prefix, "", text, ellipsis, false);
        oneInputSharedContent("", part1, text, ellipsis, false);
        oneInputSharedContent(prefix, part1, "", ellipsis, true);
    }

    private void oneInputSharedContent(String prefix, String part1, String text, String ellipsis, boolean hasAdditionalContent) {
        String subject = prefix + part1 + ellipsis;
        assertEquals(part1 + ellipsis, TimelineActivity.stripBeginning(subject));
        assertEquals(String.valueOf(prefix + part1).trim(), TimelineActivity.stripEllipsis(subject));
        assertEquals(part1, TimelineActivity.stripEllipsis(TimelineActivity.stripBeginning(subject)));
        assertEquals(hasAdditionalContent, TimelineActivity.subjectHasAdditionalContent(subject, text));
    }
}
