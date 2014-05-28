/* Copyright (C) 2014 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 * 
 * LearnLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * LearnLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with LearnLib; if not, see
 * <http://www.gnu.de/documents/lgpl.en.html>.
 */
package de.learnlib.eqtests.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

import net.automatalib.automata.concepts.OutputAutomaton;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import de.learnlib.api.EquivalenceOracle;
import de.learnlib.api.MembershipOracle;
import de.learnlib.oracles.DefaultQuery;

/**
 *
 * @author Maik Merten
 */
public class RandomWordsEQOracle<I, D, A extends OutputAutomaton<?, I, ?, D>> implements EquivalenceOracle<A, I, D> {

	public static class DFARandomWordsEQOracle<I> extends RandomWordsEQOracle<I,Boolean,DFA<?,I>>
			implements DFAEquivalenceOracle<I> {
		public DFARandomWordsEQOracle(MembershipOracle<I, Boolean> mqOracle,
				int minLength, int maxLength, int maxTests, Random random) {
			super(mqOracle, minLength, maxLength, maxTests, random);
		}
		public DFARandomWordsEQOracle(MembershipOracle<I, Boolean> mqOracle,
				int minLength, int maxLength, int maxTests, Random random, int batchSize) {
			super(mqOracle, minLength, maxLength, maxTests, random, batchSize);
		}
	}
	
	public static class MealyRandomWordsEQOracle<I,O> extends RandomWordsEQOracle<I,Word<O>,MealyMachine<?,I,?,O>>
			implements MealyEquivalenceOracle<I,O> {
		public MealyRandomWordsEQOracle(MembershipOracle<I, Word<O>> mqOracle,
				int minLength, int maxLength, int maxTests, Random random) {
			super(mqOracle, minLength, maxLength, maxTests, random);
		}
		public MealyRandomWordsEQOracle(MembershipOracle<I, Word<O>> mqOracle,
				int minLength, int maxLength, int maxTests, Random random, int batchSize) {
			super(mqOracle, minLength, maxLength, maxTests, random, batchSize);
		}
	}

	private static final Logger LOGGER = Logger.getLogger(RandomWordsEQOracle.class.getName());

	private MembershipOracle<I, D> oracle;
	private int maxTests, minLength, maxLength;
	private final Random random;
	private final int batchSize;

	public RandomWordsEQOracle(MembershipOracle<I, D> mqOracle, int minLength, int maxLength, int maxTests, Random random, int batchSize) {
		this.oracle = mqOracle;
		this.maxTests = maxTests;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.random = random;
		this.batchSize = batchSize;
	}

	public RandomWordsEQOracle(MembershipOracle<I, D> mqOracle, int minLength, int maxLength, int maxTests, Random random) {
		this(mqOracle, minLength, maxLength, maxTests, random, 1);
	}

	@Override
	public DefaultQuery<I, D> findCounterExample(A hypothesis, Collection<? extends I> inputs) {
		// Fail fast on empty inputs
		if(inputs.isEmpty()) {
			LOGGER.warning("Passed empty set of inputs to equivalence oracle; no counterexample can be found!");
			return null;
		}
		
		List<? extends I> symbolList;
		if (inputs instanceof List) {
			symbolList = (List<? extends I>) inputs;
		} else {
			symbolList = new ArrayList<>(inputs);
		}
		
		int numSyms = symbolList.size();

		final Collection<DefaultQuery<I,D>> queryBatch = new ArrayList<>(batchSize);

		for (int i = 0; i < maxTests; ++i) {
			int length = minLength + random.nextInt((maxLength - minLength) + 1);

			WordBuilder<I> testtrace = new WordBuilder<>(length);
			for (int j = 0; j < length; ++j) {
				int symidx = random.nextInt(numSyms);
				I sym = symbolList.get(symidx);
				testtrace.append(sym);
			}

			final DefaultQuery<I, D> query = new DefaultQuery<>(testtrace.toWord());
			queryBatch.add(query);
			
			final boolean batchFilled = queryBatch.size() >= batchSize;
			final boolean maxTestsReached = i >= maxTests - 1;

			if(batchFilled || maxTestsReached) {
				// query oracle
				oracle.processQueries(queryBatch);

				for (final DefaultQuery<I, D> ioQuery : queryBatch) {
					D oracleoutput = ioQuery.getOutput();

					// trace hypothesis
					D hypOutput = hypothesis.computeOutput(ioQuery.getInput());

					// compare output of hypothesis and oracle
					if (!Objects.equals(oracleoutput, hypOutput)) {
						return ioQuery;
					}
				}

				queryBatch.clear();
			}
		}

		// no counterexample found
		return null;
	}
}
