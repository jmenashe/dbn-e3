# Authors: Majid Alkaee Taleghan, Mark Crowley, Thomas Dietterich
# Invasive Species Project
# 2012 Oregon State University
# Send code issues to: alkaee@gmail.com
# Date: 1/1/13:7:51 PM
#
# I used some of Brian Tanner's experiment code for invasive experiment.
#

import numpy
from numpy.numarray import array

import random

import math
import rlglue.RLGlue as RLGlue

from datetime import datetime
import os

REACHES=4
HABITATS=3

EPISODES_PER_TEST = 5
NUMBER_OF_TESTS = 20

RESULTS_DIR = "invasive_results"

rand = random.Random(1)
STARTING_STATE = array([rand.randint(1, 3) for i in xrange(REACHES * HABITATS)])
STARTING_STATE = ",".join(map(str, STARTING_STATE))


def run_test():
    statistics = []

    for i in range(0, NUMBER_OF_TESTS):
        this_score = run_some_episodes(EPISODES_PER_TEST)
        printScore((i + 1) * EPISODES_PER_TEST, this_score)
        statistics.append(this_score)

    saveResultToCSV(
        statistics,
        REACHES,
        HABITATS,
        "results_%s.csv" % datetime.now().replace(microsecond=0).isoformat('_')
    )


def run_some_episodes(n = EPISODES_PER_TEST):
    sum = 0
    sum_of_squares = 0
    
    for i in range(0, n):
        RLGlue.RL_env_message("set-start-state " + STARTING_STATE)
        RLGlue.RL_start()
        RLGlue.RL_episode(100)

        this_return = RLGlue.RL_return()
        sum += this_return
        sum_of_squares += this_return ** 2

    mean = sum / n
    variance = (sum_of_squares - n * mean * mean) / (n - 1.0)
    standard_dev = math.sqrt(variance)

    return mean, standard_dev


def saveResultToCSV(statistics, reaches, habitats, fileName):
    filepath = os.path.join(RESULTS_DIR, fileName)
    if not os.path.exists(RESULTS_DIR):
        os.makedirs(RESULTS_DIR)

    numpy.savetxt(filepath, statistics, delimiter=",")

    with open(filepath, "a") as f:
        f.write("# Reaches: %s, Habitats: %s" % (reaches, habitats))


def printScore(afterEpisodes, score_tuple):
    print "%d\t\t%.2f\t\t%.2f" % (afterEpisodes, score_tuple[0], score_tuple[1])


RLGlue.RL_init()

print "Using a fixed start state for each episode."
print STARTING_STATE

print "-------------------------------------------------------------------------"
print "Will learn and evaluate for %d episodes" % (EPISODES_PER_TEST)
print "After Episode\tMean Return\tStandard Deviation"
print "-------------------------------------------------------------------------"

run_test()
RLGlue.RL_cleanup()
print "\nExperiment Complete."