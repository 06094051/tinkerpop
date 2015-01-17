package com.tinkerpop.gremlin.process.util;

import com.tinkerpop.gremlin.process.Step;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.marker.Reversible;
import com.tinkerpop.gremlin.process.graph.marker.TraversalHolder;
import com.tinkerpop.gremlin.process.traverser.TraverserRequirement;
import com.tinkerpop.gremlin.structure.Graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TraversalHelper {

    public static boolean isReversible(final Traversal.Admin<?, ?> traversal) {
        return !traversal.getSteps().stream().filter(step -> !(step instanceof Reversible)).findAny().isPresent();
    }

    public static <S, E> Step<S, E> getStepByLabel(final String label, final Traversal.Admin<?, ?> traversal) {
        return traversal.getSteps().stream()
                .filter(step -> step.getLabel().isPresent())
                .filter(step -> label.equals(step.getLabel().get()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("The provided step label does not exist: " + label));
    }

    public static <S, E> Step<S, E> getStepById(final String id, final Traversal.Admin<?, ?> traversal) {
        return traversal.getSteps().stream()
                .filter(step -> id.equals(step.getId()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("The provided step id does not exist: " + id));
    }


    public static <S, E> Optional<Step<S, E>> getStepByIdRecurssively(final String id, final Traversal.Admin<?, ?> traversal) {
        for (final Step<?, ?> step : traversal.getSteps()) {
            if (step.getId().equals(id))
                return Optional.of((Step) step);
            else if (step instanceof TraversalHolder) {
                for (final Traversal<?, ?> nest : ((TraversalHolder<?, ?>) step).getTraversals()) {
                    final Optional<Step<S, E>> optional = TraversalHelper.getStepByIdRecurssively(id, nest.asAdmin());
                    if (optional.isPresent())
                        return optional;
                }
            }
        }
        return Optional.empty();
    }

    public static boolean hasLabel(final String label, final Traversal.Admin<?, ?> traversal) {
        return traversal.getSteps().stream()
                .filter(step -> step.getLabel().isPresent())
                .filter(step -> label.equals(step.getLabel().get()))
                .findAny().isPresent();
    }

    public static List<String> getLabelsUpTo(final Step<?, ?> step, final Traversal.Admin<?, ?> traversal) {
        final List<String> labels = new ArrayList<>();
        for (final Step<?, ?> temp : traversal.getSteps()) {
            if (temp == step) break;
            temp.getLabel().ifPresent(labels::add);
        }
        return labels;
    }

    public static List<Step<?, ?>> getStepsUpTo(final Step<?, ?> step, final Traversal.Admin<?, ?> traversal) {
        final List<Step<?, ?>> steps = new ArrayList<>();
        for (final Step temp : traversal.getSteps()) {
            if (temp == step) break;
            steps.add(temp);
        }
        return steps;
    }

    public static <S, E> Step<S, ?> getStart(final Traversal.Admin<S, E> traversal) {
        return traversal.getSteps().size() == 0 ? EmptyStep.instance() : traversal.getSteps().get(0);
    }

    public static <S, E> Step<?, E> getEnd(final Traversal.Admin<S, E> traversal) {
        return traversal.getSteps().size() == 0 ? EmptyStep.instance() : traversal.getSteps().get(traversal.getSteps().size() - 1);
    }

    public static boolean areEqual(final Iterator a, final Iterator b) {
        while (a.hasNext() || b.hasNext()) {
            if (a.hasNext() != b.hasNext())
                return false;
            if (!a.next().equals(b.next()))
                return false;
        }
        return true;
    }

    public static <S, E> Step<?, E> insertTraversal(final int insertIndex, final Traversal.Admin<S, E> insertTraversal, final Traversal.Admin<?, ?> traversal) {
        if (0 == traversal.getSteps().size()) {
            Step currentStep = EmptyStep.instance();
            for (final Step insertStep : insertTraversal.getSteps()) {
                currentStep = insertStep;
                traversal.addStep(insertStep);
            }
            return currentStep;
        } else {
            Step currentStep = traversal.getSteps().get(insertIndex);
            for (final Step insertStep : insertTraversal.getSteps()) {
                TraversalHelper.insertAfterStep(insertStep, currentStep, traversal);
                currentStep = insertStep;
            }
            return currentStep;
        }
    }

    public static <S, E> Step<?, E> insertTraversal(final Step<?, S> previousStep, final Traversal.Admin<S, E> insertTraversal, final Traversal.Admin<?, ?> traversal) {
        return TraversalHelper.insertTraversal(traversal.getSteps().indexOf(previousStep), insertTraversal, traversal);
    }

    public static <S, E> void insertBeforeStep(final Step<S, E> step, final Step<?, S> afterStep, final Traversal.Admin<?, ?> traversal) {
        traversal.addStep(traversal.getSteps().indexOf(afterStep), step);
    }

    public static <S, E> void insertAfterStep(final Step<S, E> step, final Step<?, E> beforeStep, final Traversal.Admin<?, ?> traversal) {
        traversal.addStep(traversal.getSteps().indexOf(beforeStep) + 1, step);
    }

    public static <S, E> void replaceStep(final Step<S, E> removeStep, final Step<S, E> insertStep, final Traversal.Admin<?, ?> traversal) {
        traversal.addStep(traversal.getSteps().indexOf(removeStep), insertStep);
        traversal.removeStep(removeStep);
    }

    public static void reLinkSteps(final Traversal.Admin<?, ?> traversal) {
        final List<Step> steps = traversal.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            final Step previousStep = i > 0 ? steps.get(i - 1) : null;
            final Step currentStep = steps.get(i);
            final Step nextStep = i < steps.size() - 1 ? steps.get(i + 1) : null;
            currentStep.setPreviousStep(null != previousStep ? previousStep : EmptyStep.instance());
            currentStep.setNextStep(null != nextStep ? nextStep : EmptyStep.instance());
        }
    }

    public static String makeStepString(final Step<?, ?> step, final Object... arguments) {
        final StringBuilder builder = new StringBuilder(step.getClass().getSimpleName());
        final List<String> strings = Stream.of(arguments)
                .filter(o -> null != o)
                .filter(o -> {
                    if (o instanceof FunctionRing) {
                        return ((FunctionRing) o).size() > 0;
                    } else if (o instanceof Collection) {
                        return ((Collection) o).size() > 0;
                    } else if (o instanceof Map) {
                        return ((Map) o).size() > 0;
                    } else {
                        return true;
                    }
                })
                .map(o -> {
                    final String string = o.toString();
                    return string.contains("$") ? "lambda" : string;
                }).filter(o -> !Graph.Hidden.isHidden(o))
                .collect(Collectors.toList());
        if (strings.size() > 0) {
            builder.append("(");
            builder.append(String.join(",", strings));
            builder.append(")");
        }
        step.getLabel().ifPresent(label -> builder.append("@").append(label));
        // builder.append("^").append(step.getId());
        return builder.toString();
    }

    public static String makeTraversalString(final Traversal.Admin<?, ?> traversal) {
        final List<Step> temp = new ArrayList<>();
        Step currentStep = TraversalHelper.getStart(traversal);
        while (!(currentStep instanceof EmptyStep)) {
            temp.add(currentStep);
            currentStep = currentStep.getNextStep();
        }
        return temp.toString();
    }

    public static <S extends Step> List<S> getStepsOfClass(final Class<S> stepClass, final Traversal.Admin<?, ?> traversal) {
        return (List) traversal.getSteps().stream().filter(step -> step.getClass().equals(stepClass)).collect(Collectors.toList());
    }

    public static <S extends Step> List<S> getStepsOfAssignableClass(final Class stepClass, final Traversal.Admin<?, ?> traversal) {
        return (List) traversal.getSteps().stream().filter(step -> stepClass.isAssignableFrom(step.getClass())).collect(Collectors.toList());
    }

    public static <C extends Step> Optional<C> getLastStepOfAssignableClass(final Class<C> classToGet, final Traversal.Admin<?, ?> traversal) {
        final List<C> steps = (List) traversal.getSteps().stream().filter(step -> classToGet.isAssignableFrom(step.getClass())).collect(Collectors.toList());
        return steps.size() == 0 ? Optional.empty() : Optional.of(steps.get(steps.size() - 1));
    }

    public static <S extends Step> List<S> getStepsOfAssignableClassRecurssively(final Class stepClass, final Traversal.Admin<?, ?> traversal) {
        final List<S> list = new ArrayList<>();
        for (final Step<?, ?> step : traversal.getSteps()) {
            if (stepClass.isAssignableFrom(step.getClass()))
                list.add((S) step);
            if (step instanceof TraversalHolder) {
                for (final Traversal<?, ?> nest : ((TraversalHolder<?, ?>) step).getTraversals()) {
                    list.addAll(TraversalHelper.getStepsOfAssignableClassRecurssively(stepClass, nest.asAdmin()));
                }
            }
        }
        return list;
    }

    public static boolean hasStepOfClass(final Class<? extends Step> stepClass, final Traversal.Admin<?, ?> traversal) {
        for (final Step<?, ?> step : traversal.getSteps()) {
            if (step.getClass().equals(stepClass)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasStepOfAssignableClass(final Class superClass, final Traversal.Admin<?, ?> traversal) {
        for (final Step<?, ?> step : traversal.getSteps()) {
            if (superClass.isAssignableFrom(step.getClass())) {
                return true;
            }
        }
        return false;
    }

    public static void verifySideEffectKeyIsNotAStepLabel(final String key, final Traversal.Admin<?, ?> traversal) {
        if (TraversalHelper.hasLabel(key, traversal))
            throw new IllegalArgumentException("The provided side effect key is already used as a step label: " + key);
    }

    public static void verifyStepLabelIsNotASideEffectKey(final String label, final Traversal.Admin<?, ?> traversal) {
        if (traversal.getSideEffects().exists(label))
            throw new IllegalArgumentException("The provided step label is already used as a side effect key: " + label);
    }

    public static <S> void addToCollection(final Collection<S> collection, final S s, final long bulk) {
        if (collection instanceof BulkSet) {
            ((BulkSet<S>) collection).add(s, bulk);
        } else if (collection instanceof Set) {
            collection.add(s);
        } else {
            for (long i = 0; i < bulk; i++) {
                collection.add(s);
            }
        }
    }

    public static <S> void addToCollectionUnrollIterator(final Collection<S> collection, final S s, final long bulk) {
        if (s instanceof Iterator) {
            ((Iterator<S>) s).forEachRemaining(r -> addToCollection(collection, r, bulk));
        } else if (s instanceof Iterable) {
            ((Iterable<S>) s).forEach(r -> addToCollection(collection, r, bulk));
        } else {
            addToCollection(collection, s, bulk);
        }
    }

    /**
     * Returns the name of <i>step</i> truncated to <i>maxLength</i>. An ellipses is appended when the name exceeds
     * <i>maxLength</i>.
     *
     * @param step
     * @param maxLength Includes the 3 "..." characters that will be appended when the length of the name exceeds
     *                  maxLength.
     * @return short step name.
     */
    public static String getShortName(final Step step, final int maxLength) {
        final String name = step.toString();
        if (name.length() > maxLength)
            return name.substring(0, maxLength - 3) + "...";
        return name;
    }

    public static Set<TraverserRequirement> getRequirements(final Traversal.Admin<?, ?> traversal) {
        final Set<TraverserRequirement> requirements = traversal.getSteps().stream()
                .flatMap(step -> ((Step<?, ?>) step).getRequirements().stream())
                .collect(Collectors.toSet());
        if (traversal.getSideEffects().keys().size() > 0)
            requirements.add(TraverserRequirement.SIDE_EFFECTS);
        if (traversal.getSideEffects().getSackInitialValue().isPresent())
            requirements.add(TraverserRequirement.SACK);
        return requirements;
    }

    public static void reIdSteps(final StepPosition stepPosition, final Traversal.Admin<?, ?> traversal) {
        stepPosition.x = 0;
        stepPosition.y = -1;
        stepPosition.z = -1;
        stepPosition.parentId = null;
        Traversal.Admin<?, ?> current = traversal;
        while (!(current instanceof EmptyTraversal)) {
            stepPosition.y++;
            final TraversalHolder<?, ?> holder = current.getTraversalHolder();
            if (null == stepPosition.parentId && !(holder instanceof EmptyStep))
                stepPosition.parentId = holder.asStep().getId();
            if (-1 == stepPosition.z) {
                for (int i = 0; i < holder.getTraversals().size(); i++) {
                    if (holder.getTraversals().get(i) == current) {
                        stepPosition.z = i;
                    }
                }
            }
            current = holder.asStep().getTraversal().asAdmin();
        }
        if (-1 == stepPosition.z) stepPosition.z = 0;
        if (null == stepPosition.parentId) stepPosition.parentId = "";
        for (final Step<?, ?> step : traversal.getSteps()) {
            step.setId(stepPosition.nextXId());
        }
    }

    public static Traversal<?, ?> getRootTraversal(Traversal.Admin<?, ?> traversal) {
        while (!((traversal.getTraversalHolder()) instanceof EmptyStep)) {
            traversal = traversal.getTraversalHolder().asStep().getTraversal().asAdmin();
        }
        return traversal;
    }
}
