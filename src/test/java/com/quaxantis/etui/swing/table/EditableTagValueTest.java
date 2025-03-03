package com.quaxantis.etui.swing.table;

import com.quaxantis.etui.TagValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class EditableTagValueTest {
    private static final String GROUP = "testGroup";
    private static final String TAG = "testTag";
    private static final String VALUE = "testValue";
    private static final String DELETED_TAG_VALUE = "-";
    private static final Comparator<TagValue> COMPARATOR = Comparator.comparing(TagValue::groupName).thenComparing(TagValue::tagName).thenComparing(TagValue::value);

    private final EditableTagValue tag = EditableTagValue.of(TagValue.of(GROUP, TAG, VALUE));

    @Nested
    @DisplayName("an original tag")
    class OriginalTagValueTest {
        @Test
        @DisplayName("is not changed, added or deleted, cannot be reset and returns original values")
        void testOriginalTag() {
            assertThat(tag)
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(false, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("canBeReset").returns(false, EditableTagValue::canBeReset)
                    .describedAs("canBeEdited").returns(true, EditableTagValue::canBeEdited)
                    .describedAs("canBeDeleted").returns(true, EditableTagValue::canBeDeleted)
                    .describedAs("has no changes").satisfies(hasNoChanges())
            ;
        }

        @Test
        @DisplayName("that is changed with the same value, remains unchanged")
        void testWithSameValueIsSameInstance() {
            assertThat(tag.withValue(VALUE)).isSameAs(tag);
        }

        @Test
        @DisplayName("that is replaced with the same tag value, remains unchanged")
        void testReplaceWithSameTagValueIsSameInstance() {
            assertThat(tag.replaceWith(TagValue.of(GROUP, TAG, VALUE))).isSameAs(tag);
        }

        @Test
        @DisplayName("that is reset remains unmodified (cannot be reset)")
        void testOriginalResetTag() {
            assertThat(tag.reset())
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(false, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has no changes").satisfies(hasNoChanges())
            ;
        }

        @Test
        @DisplayName("that is merged returns the new value")
        void testOriginalMergedTag() {
            assertThat(tag.mergeWithValue("anotherValue"))
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns("anotherValue", EditableTagValue::value)
                    .returns("anotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("canBeReset").returns(true, EditableTagValue::canBeReset)
                    .describedAs("canBeEdited").returns(true, EditableTagValue::canBeEdited)
                    .describedAs("canBeDeleted").returns(true, EditableTagValue::canBeDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, "anotherValue")))
            ;
        }

        @Test
        @DisplayName("that is merged with the same value, remains unchanged")
        void testMergeWithSameValueIsSameInstance() {
            assertThat(tag.mergeWithValue(VALUE)).isSameAs(tag);
        }
    }

    @Nested
    @DisplayName("an added tag")
    class AddedTagValueTest {

        private final EditableTagValue tag = EditableTagValue.ofAdded(TagValue.of(GROUP, TAG, VALUE));

        @Test
        @DisplayName("is changed and added, cannot be reset and returns original values")
        void testAddedTag() {
            assertThat(tag)
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(true, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("canBeReset").returns(false, EditableTagValue::canBeReset)
                    .describedAs("canBeEdited").returns(true, EditableTagValue::canBeEdited)
                    .describedAs("canBeDeleted").returns(true, EditableTagValue::canBeDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, VALUE)))
            ;
        }

        @Test
        @DisplayName("that is edited returns another value")
        void testAddedEditedTag() {
            assertThat(tag.withValue("anotherValue"))
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns("anotherValue", EditableTagValue::value)
                    .returns("anotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(true, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, "anotherValue")))
            ;
        }

        @Test
        @DisplayName("that is edited and deleted disappears")
        void testAddedEditedDeletedTag() {
            assertThat(tag.withValue("anotherValue").delete()).isEmpty();
        }

        @Test
        @DisplayName("that is replaced returns another tag value")
        void testAddedReplacedTag() {
            assertThat(tag.replaceWith(TagValue.of("anotherGroup", "anotherTag", "anotherValue")))
                    .returns("anotherGroup", EditableTagValue::groupName)
                    .returns("anotherTag", EditableTagValue::tagName)
                    .returns("anotherValue", EditableTagValue::value)
                    .returns("anotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(true, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of("anotherGroup", "anotherTag", "anotherValue")))
            ;
        }

        @Test
        @DisplayName("that is replaced and deleted disappears")
        void testAddedReplacedDeletedTag() {
            assertThat(tag.replaceWith(TagValue.of("anotherGroup", "anotherTag", "anotherValue")).delete()).isEmpty();
        }

        @Test
        @DisplayName("that is replaced and reset returns the original value")
        void testAddedReplacedResetTag() {
            assertThat(tag.replaceWith(TagValue.of("anotherGroup", "anotherTag", "anotherValue")).reset())
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(true, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, VALUE)))
            ;
        }

        @Test
        @DisplayName("that is deleted disappears")
        void testAddedDeletedTag() {
            assertThat(tag.delete()).isEmpty();
        }

        @Test
        @DisplayName("that is reset remains unmodified (cannot be reset)")
        void testAddedResetTag() {
            assertThat(tag.reset())
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(true, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, VALUE)))
            ;
        }

        @Test
        @DisplayName("that is edited and reset returns the original value")
        void testAddedEditedResetTag() {
            assertThat(tag.withValue("anotherValue").reset())
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(true, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, VALUE)))
            ;
        }

        @Test
        @DisplayName("that is merged returns the new value")
        void testOriginalMergedTag() {
            assertThat(tag.mergeWithValue("anotherValue"))
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns("anotherValue", EditableTagValue::value)
                    .returns("anotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(true, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("canBeReset").returns(true, EditableTagValue::canBeReset)
                    .describedAs("canBeEdited").returns(true, EditableTagValue::canBeEdited)
                    .describedAs("canBeDeleted").returns(true, EditableTagValue::canBeDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, "anotherValue")))
            ;
        }

    }

    @Nested
    @DisplayName("an edited tag")
    class EditedTagValueTest {

        @Test
        @DisplayName("is changed and returns another value")
        void testEditedTag() {
            assertThat(tag.withValue("anotherValue"))
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns("anotherValue", EditableTagValue::value)
                    .returns("anotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("canBeReset").returns(true, EditableTagValue::canBeReset)
                    .describedAs("canBeEdited").returns(true, EditableTagValue::canBeEdited)
                    .describedAs("canBeDeleted").returns(true, EditableTagValue::canBeDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, "anotherValue")))
            ;
        }

        @Test
        @DisplayName("that is edited again returns the new value")
        void testEditedEditedTag() {
            assertThat(tag.withValue("anotherValue").withValue("yetAnotherValue"))
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns("yetAnotherValue", EditableTagValue::value)
                    .returns("yetAnotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, "yetAnotherValue")))
            ;
        }

        @Test
        @DisplayName("that is merged returns the new value")
        void testEditedMergedTag() {
            assertThat(tag.withValue("anotherValue").mergeWithValue("yetAnotherValue"))
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns("yetAnotherValue", EditableTagValue::value)
                    .returns("yetAnotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, "yetAnotherValue")))
            ;
        }


        @Test
        @DisplayName("that is replaced returns the new tag value")
        void testEditedReplacedTag() {
            assertThat(tag.withValue("anotherValue").replaceWith(TagValue.of("anotherGroup", "anotherTag", "yetAnotherValue")))
                    .returns("anotherGroup", EditableTagValue::groupName)
                    .returns("anotherTag", EditableTagValue::tagName)
                    .returns("yetAnotherValue", EditableTagValue::value)
                    .returns("yetAnotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(
                            hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE),
                                       TagValue.of("anotherGroup", "anotherTag", "yetAnotherValue")))
            ;
        }

        @Test
        @DisplayName("that is reset is not changed and returns the original value")
        void testEditedResetTag() {
            assertThat(tag.withValue("anotherValue").reset())
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(false, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has no changes").satisfies(hasNoChanges())
            ;
        }

        @Test
        @DisplayName("that is edited again and then reset is not changed and returns the original value")
        void testEditedTwiceResetTag() {
            assertThat(tag.withValue("anotherValue").withValue("yetAnotherValue").reset())
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(false, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has no changes").satisfies(hasNoChanges())
            ;
        }

        @Test
        @DisplayName("that is deleted is changed and deleted and returns the original display value")
        void testEditedDeletedTag() {
            assertThat(tag.withValue("anotherValue").delete())
                    .get()
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(DELETED_TAG_VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(true, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE)))
            ;
        }


    }

    @Nested
    @DisplayName("a replaced tag")
    class ReplacedTagValueTest {

        private final TagValue anotherTag = TagValue.of("anotherGroup", "anotherTag", "anotherValue");

        @Test
        @DisplayName("is changed and returns another tag and value")
        void testReplacedTag() {
            assertThat(tag.replaceWith(anotherTag))
                    .returns("anotherGroup", EditableTagValue::groupName)
                    .returns("anotherTag", EditableTagValue::tagName)
                    .returns("anotherValue", EditableTagValue::value)
                    .returns("anotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("canBeReset").returns(true, EditableTagValue::canBeReset)
                    .describedAs("canBeEdited").returns(true, EditableTagValue::canBeEdited)
                    .describedAs("canBeDeleted").returns(true, EditableTagValue::canBeDeleted)
                    .describedAs("has changes").satisfies(
                            hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE),
                                       TagValue.of("anotherGroup", "anotherTag", "anotherValue")))
            ;
        }

        @Test
        @DisplayName("that is replaced again returns the new tag and value")
        void testReplacedTwiceTag() {
            assertThat(tag.replaceWith(anotherTag).replaceWith(TagValue.of("yetAnotherGroup", "yetAnotherTag", "yetAnotherValue")))
                    .returns("yetAnotherGroup", EditableTagValue::groupName)
                    .returns("yetAnotherTag", EditableTagValue::tagName)
                    .returns("yetAnotherValue", EditableTagValue::value)
                    .returns("yetAnotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has changes").satisfies(
                            hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE),
                                       TagValue.of("yetAnotherGroup", "yetAnotherTag", "yetAnotherValue")))
            ;
        }

        @Test
        @DisplayName("that is replaced again and then reset is not changed and returns the original value")
        void testReplacedTwiceResetTag() {
            assertThat(tag.replaceWith(anotherTag).replaceWith(TagValue.of("yetAnotherGroup", "yetAnotherTag", "yetAnotherValue")).reset())
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("has no changes").satisfies(hasNoChanges())
            ;
        }

        @Test
        @DisplayName("that is edited returns the new value")
        void testReplacedEditedTag() {
            assertThat(tag.replaceWith(anotherTag).withValue("yetAnotherValue"))
                    .returns("anotherGroup", EditableTagValue::groupName)
                    .returns("anotherTag", EditableTagValue::tagName)
                    .returns("yetAnotherValue", EditableTagValue::value)
                    .returns("yetAnotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has changes").satisfies(
                            hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE),
                                       TagValue.of("anotherGroup", "anotherTag", "yetAnotherValue")))
            ;
        }

        @Test
        @DisplayName("that is merged returns the new value")
        void testReplacedMergedTag() {
            assertThat(tag.replaceWith(anotherTag).mergeWithValue("yetAnotherValue"))
                    .returns("anotherGroup", EditableTagValue::groupName)
                    .returns("anotherTag", EditableTagValue::tagName)
                    .returns("yetAnotherValue", EditableTagValue::value)
                    .returns("yetAnotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has changes").satisfies(
                            hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE),
                                       TagValue.of("anotherGroup", "anotherTag", "yetAnotherValue")))
            ;
        }

        @Test
        @DisplayName("that is reset is not changed and returns the original value")
        void testEditedResetTag() {
            assertThat(tag.replaceWith(anotherTag).reset())
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(false, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has no changes").satisfies(hasNoChanges())
            ;
        }

        @Test
        @DisplayName("that is deleted is changed and deleted and returns the original tag and display value")
        void testReplacedDeletedTag() {
            assertThat(tag.replaceWith(anotherTag).delete())
                    .get()
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(DELETED_TAG_VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(true, EditableTagValue::isDeleted)
                    .describedAs("has changes").satisfies(hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE)))
            ;
        }

    }

    @Nested
    @DisplayName("a deleted tag")
    class DeletedTagValueTest {

        @Test
        @DisplayName("is changed and deleted, cannot be edited or deleted, and returns a special value but the original display value")
        void testDeletedTag() {
            assertThat(tag.delete())
                    .get()
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(DELETED_TAG_VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(true, EditableTagValue::isDeleted)
                    .describedAs("canBeReset").returns(true, EditableTagValue::canBeReset)
                    .describedAs("canBeEdited").returns(false, EditableTagValue::canBeEdited)
                    .describedAs("canBeDeleted").returns(false, EditableTagValue::canBeDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE)))
            ;
        }

        @Test
        @DisplayName("that is reset is not changed and not deleted and returns the original value")
        void testDeletedResetTag() {
            assertThat(tag.delete().orElseThrow().reset())
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(false, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has no changes").satisfies(hasNoChanges())
            ;
        }

        @Test
        @DisplayName("that is edited remains unmodified (cannot be edited)")
        void testDeletedEditedTag() {
            assertThat(tag.delete().orElseThrow().withValue("ignoredValue"))
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(DELETED_TAG_VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(true, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE)))
            ;
        }

        @Test
        @DisplayName("that is replaced remains unmodified (cannot be edited)")
        void testDeletedReplacedTag() {
            assertThat(tag.delete().orElseThrow().replaceWith(TagValue.of("anotherGroup", "anotherTag", "yetAnotherValue")))
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(DELETED_TAG_VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(true, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE)))
            ;
        }

        @Test
        @DisplayName("that is merged returns the new value")
        void testEditedMergedTag() {
            assertThat(tag.delete().orElseThrow().mergeWithValue("yetAnotherValue"))
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns("yetAnotherValue", EditableTagValue::value)
                    .returns("yetAnotherValue", EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(false, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, "yetAnotherValue")))
            ;
        }

        @Test
        @DisplayName("that is deleted again remains unmodified (cannot be deleted twice)")
        void testDeletedDeletedTag() {
            assertThat(tag.delete().orElseThrow().delete())
                    .get()
                    .returns(GROUP, EditableTagValue::groupName)
                    .returns(TAG, EditableTagValue::tagName)
                    .returns(DELETED_TAG_VALUE, EditableTagValue::value)
                    .returns(VALUE, EditableTagValue::displayValue)
                    .describedAs("isChanged").returns(true, EditableTagValue::isChanged)
                    .describedAs("isAdded").returns(false, EditableTagValue::isAdded)
                    .describedAs("isDeleted").returns(true, EditableTagValue::isDeleted)
                    .describedAs("has a change").satisfies(hasChanges(TagValue.of(GROUP, TAG, DELETED_TAG_VALUE)))
            ;
        }
    }

    private Consumer<EditableTagValue> hasChanges(TagValue... tagValues) {
        return editableTagValue -> {
            assertThat(collectChanges(editableTagValue))
                    .usingElementComparator(COMPARATOR)
                    .containsExactlyInAnyOrder(tagValues);
        };
    }

    private Consumer<EditableTagValue> hasNoChanges() {
        return editableTagValue -> {
            assertThat(collectChanges(editableTagValue)).isEmpty();
        };
    }

    private List<TagValue> collectChanges(EditableTagValue editableTagValue) {
        List<TagValue> list = new ArrayList<>();
        editableTagValue.pushChanges(list::add);
        return list;
    }
}
