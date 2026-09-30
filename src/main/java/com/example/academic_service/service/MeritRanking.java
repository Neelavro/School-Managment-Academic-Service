package com.example.academic_service.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The one ranking behind every position in the app: routine and annual results (and their PDFs), the progress
 * report, the student portal and both merit lists. User decisions (2026-09-30):
 *
 *   order            — GPA, then total, when the class uses GPA; total, then GPA, when it doesn't (without a grading
 *                      policy every GPA is 0, so that is total alone); then lower roll, then lower enrollment id.
 *   who              — passed students only; a failed student has no position (null), so there are no gaps.
 *   class position   — within the student's group (a class without groups is one group).
 *   shift position   — within the group and gender section, only when the class has more than one gender section.
 *   section position — within the group and section, only for students in a section.
 *
 * Callers pass every student of the class (or of the group being reported), not a filtered list: filters
 * decide who is shown, never the positions.
 */
public final class MeritRanking {

    private MeritRanking() {}

    /** One student as the ranking sees them. */
    public record Candidate(Long enrollmentId, Integer groupId, Integer genderSectionId, Long sectionId,
                            Integer classRoll, Double gpa, BigDecimal total, boolean passed) {}

    /** A student's positions; each is null when it doesn't apply. */
    public record Positions(Integer classPosition, Integer shiftPosition, Integer sectionPosition) {
        public static final Positions NONE = new Positions(null, null, null);
    }

    /**
     * The merit order (see the class comment), for any row type.
     */
    public static <T> Comparator<T> order(Function<T, Double> gpa, Function<T, BigDecimal> total,
                                          Function<T, Integer> roll, Function<T, Long> id, boolean gpaFirst) {
        Comparator<T> byGpa = Comparator.comparing((T t) -> gpa.apply(t) != null ? gpa.apply(t) : 0.0, Comparator.reverseOrder());
        Comparator<T> byTotal = Comparator.comparing((T t) -> total.apply(t) != null ? total.apply(t) : BigDecimal.ZERO, Comparator.reverseOrder());
        return (gpaFirst ? byGpa.thenComparing(byTotal) : byTotal.thenComparing(byGpa))
                .thenComparing(t -> roll.apply(t) != null ? roll.apply(t) : Integer.MAX_VALUE)
                .thenComparing(id::apply);
    }

    /**
     * Positions for every candidate (enrollmentId → positions; failed students get Positions.NONE).
     *
     * @param severalShifts whether the class has more than one gender section; pass null to work it out from
     *                      the candidates (only right when they are the whole class).
     */
    public static Map<Long, Positions> rank(List<Candidate> candidates, boolean gpaFirst, Boolean severalShifts) {
        boolean shifts = severalShifts != null ? severalShifts
                : candidates.stream().map(Candidate::genderSectionId).filter(Objects::nonNull).distinct().count() > 1;
        Comparator<Candidate> merit = order(Candidate::gpa, Candidate::total, Candidate::classRoll, Candidate::enrollmentId, gpaFirst);
        List<Candidate> passed = candidates.stream().filter(Candidate::passed).toList();

        Map<Long, Integer> cls = new HashMap<>(), shift = new HashMap<>(), section = new HashMap<>();
        rankWithin(passed, merit, c -> String.valueOf(c.groupId()), cls::put);
        if (shifts)
            rankWithin(passed.stream().filter(c -> c.genderSectionId() != null).toList(), merit,
                    c -> c.groupId() + "/" + c.genderSectionId(), shift::put);
        rankWithin(passed.stream().filter(c -> c.sectionId() != null).toList(), merit,
                c -> c.groupId() + "/" + c.sectionId(), section::put);

        Map<Long, Positions> out = new HashMap<>();
        for (Candidate c : candidates)
            out.put(c.enrollmentId(), c.passed()
                    ? new Positions(cls.get(c.enrollmentId()), shift.get(c.enrollmentId()), section.get(c.enrollmentId()))
                    : Positions.NONE);
        return out;
    }

    /** Positions 1, 2, 3… inside each bucket, in merit order. */
    private static void rankWithin(List<Candidate> candidates, Comparator<Candidate> merit,
                                   Function<Candidate, String> bucket, BiConsumer<Long, Integer> set) {
        Map<String, List<Candidate>> buckets = candidates.stream()
                .collect(Collectors.groupingBy(bucket, LinkedHashMap::new, Collectors.toList()));
        for (List<Candidate> list : buckets.values()) {
            List<Candidate> sorted = new ArrayList<>(list);
            sorted.sort(merit);
            for (int i = 0; i < sorted.size(); i++) set.accept(sorted.get(i).enrollmentId(), i + 1);
        }
    }
}
