package com.example.academic_service.service;

import com.example.academic_service.service.MeritRanking.Candidate;
import com.example.academic_service.service.MeritRanking.Positions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MeritRankingTest {

    private static Candidate c(long id, Integer group, Integer gs, Long section, int roll, double gpa, int total, boolean passed) {
        return new Candidate(id, group, gs, section, roll, gpa, BigDecimal.valueOf(total), passed);
    }

    @Test
    void gpaFirstThenTotalWhenTheClassUsesGpa() {
        Map<Long, Positions> p = MeritRanking.rank(List.of(
                c(1, null, 1, null, 1, 4.50, 900, true),
                c(2, null, 1, null, 2, 5.00, 800, true),
                c(3, null, 1, null, 3, 5.00, 850, true)), true, false);
        assertEquals(1, p.get(3L).classPosition());
        assertEquals(2, p.get(2L).classPosition());
        assertEquals(3, p.get(1L).classPosition());
    }

    @Test
    void totalFirstWhenTheClassDoesNotUseGpa() {
        Map<Long, Positions> p = MeritRanking.rank(List.of(
                c(1, null, 1, null, 1, 4.50, 900, true),
                c(2, null, 1, null, 2, 5.00, 800, true)), false, false);
        assertEquals(1, p.get(1L).classPosition());
        assertEquals(2, p.get(2L).classPosition());
    }

    @Test
    void exactTieGoesToTheLowerRoll() {
        Map<Long, Positions> p = MeritRanking.rank(List.of(
                c(1, null, 1, null, 7, 5.00, 900, true),
                c(2, null, 1, null, 3, 5.00, 900, true)), true, false);
        assertEquals(1, p.get(2L).classPosition());
    }

    @Test
    void failedStudentsHaveNoPositionAndLeaveNoGap() {
        Map<Long, Positions> p = MeritRanking.rank(List.of(
                c(1, null, 1, 10L, 1, 5.00, 990, false),
                c(2, null, 1, 10L, 2, 4.00, 800, true),
                c(3, null, 1, 10L, 3, 3.00, 700, true)), true, false);
        assertEquals(Positions.NONE, p.get(1L));
        assertEquals(1, p.get(2L).classPosition());
        assertEquals(2, p.get(3L).classPosition());
        assertEquals(1, p.get(2L).sectionPosition());
    }

    @Test
    void classPositionIsWithinTheGroup() {
        Map<Long, Positions> p = MeritRanking.rank(List.of(
                c(1, 1, 1, null, 1, 5.00, 900, true),
                c(2, 2, 1, null, 2, 4.00, 800, true),
                c(3, 1, 1, null, 3, 3.00, 700, true)), true, false);
        assertEquals(1, p.get(1L).classPosition());
        assertEquals(1, p.get(2L).classPosition());
        assertEquals(2, p.get(3L).classPosition());
    }

    @Test
    void shiftPositionOnlyWithSeveralGenderSections() {
        List<Candidate> one = List.of(c(1, null, 1, null, 1, 5.00, 900, true), c(2, null, 1, null, 2, 4.00, 800, true));
        assertNull(MeritRanking.rank(one, true, null).get(1L).shiftPosition());

        Map<Long, Positions> p = MeritRanking.rank(List.of(
                c(1, null, 1, null, 1, 5.00, 900, true),
                c(2, null, 2, null, 2, 4.00, 800, true),
                c(3, null, 2, null, 3, 4.50, 850, true)), true, null);
        assertEquals(1, p.get(1L).shiftPosition());
        assertEquals(1, p.get(3L).shiftPosition());
        assertEquals(2, p.get(2L).shiftPosition());
    }

    @Test
    void sectionPositionOnlyForStudentsInASection() {
        Map<Long, Positions> p = MeritRanking.rank(List.of(
                c(1, null, 1, null, 1, 5.00, 900, true),
                c(2, null, 1, 10L, 2, 4.00, 800, true)), true, false);
        assertNull(p.get(1L).sectionPosition());
        assertEquals(1, p.get(2L).sectionPosition());
    }
}
