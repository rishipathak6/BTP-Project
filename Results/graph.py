import matplotlib.pyplot as plt
import numpy as np

# Plot 1
x1 = np.array([0, 1, 2, 3, 5, 10, 20, 30, 50, 75, 90, 100])
y1 = np.array([0.117, 0.142, 0.138, 0.177, 0.171, 0.179,
              0.276, 0.245, 0.534, 0.689, 0.724, 0.868])
x2 = np.array([0, 1, 2, 3, 5, 10, 20, 30, 50, 75, 90, 100])
y2 = np.array([0.037, 0.043, 0.039, 0.039, 0.048, 0.068,
              0.087, 0.1, 0.206, 0.279, 0.372, 0.337])

plt.subplot(2, 2, 1)
plt.plot(x1, y1, label="Average", marker='o')
plt.plot(x2, y2, label="Minimum", marker='o')

plt.title("ChaCha20 Encryption time")
plt.xlabel("Encryption Percentage")
plt.ylabel("Time (ms)")

plt.legend()

# Plot 2
x1 = np.array([0, 1, 2, 3, 5, 10, 20, 30, 50, 75, 90, 100])
y1 = np.array([0.389966401062417, 0.4019755006675567, 0.45340595939751144, 0.4022894375857339, 0.42340947148541114, 0.69987745819398,
              0.3886070093457944, 0.3741603870967742, 0.41577625754527164, 0.37664372254710854, 0.3894352722580645, 0.401435])
x2 = np.array([0, 1, 2, 3, 5, 10, 20, 30, 50, 75, 90, 100])
y2 = np.array([0.2271, 0.2365, 0.2221, 0.2262, 0.237, 0.2313,
              0.2139, 0.2045, 0.2288, 0.2307, 0.227201, 0.2342])

plt.subplot(2, 2, 2)
plt.plot(x1, y1, label="Average", marker='o')
plt.plot(x2, y2, label="Minimum", marker='o')

plt.title("RSA Decryption time")
plt.xlabel("Encryption Percentage")
plt.ylabel("Time (ms)")

plt.legend()

# Plot 3
x1 = np.array([0, 1, 2, 3, 5, 10, 20, 30, 50, 75, 90, 100])
y1 = np.array([0.11949203715992038, 0.14960420560747664, 0.15884296005239032, 0.15992050754458162, 0.17434840915119362, 0.21724735785953178,
              0.32940033377837113, 0.2850522888459059, 0.5310722334004024, 0.5741273766233767, 0.7620500296774193, 0.7629083062946139])
x2 = np.array([0, 1, 2, 3, 5, 10, 20, 30, 50, 75, 90, 100])
y2 = np.array([0.0441, 0.0505, 0.0418, 0.0457, 0.0412, 0.0724,
              0.0966, 0.1103, 0.2162, 0.2727, 0.3838, 0.3484])

plt.subplot(2, 2, 3)
plt.plot(x1, y1, label="Average", marker='o')
plt.plot(x2, y2, label="Minimum", marker='o')

plt.title("ChaCha20 Decryption time")
plt.xlabel("Encryption Percentage")
plt.ylabel("Time (ms)")

plt.legend()

# Plot 4
x1 = np.array([0, 1, 2, 3, 5, 10, 20, 30, 50, 75, 90, 100])
y1 = np.array([11.796610351692104, 11.724610146862483, 10.36039004584152, 10.431613580246914, 9.768602104774535, 9.058592240802676,
              8.86311341789052, 8.247377369439072,  9.155947551978539, 7.603351225974026,  8.64361497032258, 8.442077482154446])
x2 = np.array([0, 1, 2, 3, 5, 10, 20, 30, 50, 75, 90, 100])
y2 = np.array([3.6845, 5.4115, 5.2249, 5.6857, 3.953399,  5.1494,
              3.6151, 3.6684,  3.8231, 3.682899, 4.199899, 3.5922])

plt.subplot(2, 2, 4)
plt.plot(x1, y1, label="Average", marker='o')
plt.plot(x2, y2, label="Minimum", marker='o')

plt.title("RSA Encryption time")
plt.xlabel("Encryption Percentage")
plt.ylabel("Time (ms)")

plt.legend()

plt.show()
