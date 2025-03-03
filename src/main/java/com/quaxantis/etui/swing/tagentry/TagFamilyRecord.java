package com.quaxantis.etui.swing.tagentry;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagFamily;

import java.util.List;

record TagFamilyRecord(String label, List<? extends Tag> tags) implements TagFamily {
}
