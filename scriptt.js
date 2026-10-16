
const form = document.getElementById("studentForm");
const emailGateForm = document.getElementById("emailGateForm");
const emailInput = document.getElementById("universityEmail");
const emailGateMessage = document.getElementById("emailGateMessage");
const emailGate = document.getElementById("emailGate");
const appContent = document.getElementById("appContent");
const welcomeScreen = document.getElementById("welcomeScreen");
const welcomeStart = document.getElementById("welcomeStart");

welcomeStart.addEventListener("click", () => {
  welcomeScreen.hidden = true;
  emailGate.hidden = false;
  emailInput.focus();
});

emailGateForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const email = emailInput.value.trim();
  const universityEmailPattern = /^[^\s@]+@srmap\.edu\.in$/i;

  if (!emailInput.checkValidity() || !universityEmailPattern.test(email)) {
    emailGateMessage.textContent = "Access is limited to a valid email ending in @srmap.edu.in.";
    emailInput.setAttribute("aria-invalid", "true");
    emailInput.focus();
    return;
  }

  emailInput.removeAttribute("aria-invalid");
  emailGate.hidden = true;
  appContent.hidden = false;
});

emailInput.addEventListener("input", () => {
  emailInput.removeAttribute("aria-invalid");
  emailGateMessage.textContent = "Use an address ending in @srmap.edu.in.";
});

const nameInput = document.getElementById("studentName");
const idInput = document.getElementById("studentId");
const degreeInput = document.getElementById("degree");
const deptInput = document.getElementById("department");
const yearInput = document.getElementById("year");
const durationInput = document.getElementById("academicYears");
const photoInput = document.getElementById("photo");

const previewPhoto = document.getElementById("previewPhoto");
const photoPlaceholder = document.getElementById("photoPlaceholder");
const photoLabel = document.getElementById("photoLabel");

let cardGenerated = false;
let selectedPhotoURL = null;
let generatedPhotoURL = null;

function generateCard() {
  document.getElementById("previewName").textContent =
    nameInput.value.trim();

  document.getElementById("previewId").textContent =
    idInput.value.trim();

  document.getElementById("previewDegree").textContent =
    degreeInput.value;

  document.getElementById("previewDept").textContent =
    deptInput.value;

  document.getElementById("previewYear").textContent =
    yearInput.value;

  document.getElementById("previewDuration").textContent =
    durationInput.value.trim();

  if (generatedPhotoURL) {
    URL.revokeObjectURL(generatedPhotoURL);
    generatedPhotoURL = null;
  }

  if (selectedPhotoURL && photoInput.files[0]) {
    generatedPhotoURL = URL.createObjectURL(photoInput.files[0]);
    previewPhoto.src = generatedPhotoURL;
    previewPhoto.style.display = "block";
    photoPlaceholder.style.display = "none";
  } else {
    previewPhoto.removeAttribute("src");
    previewPhoto.style.display = "none";
    photoPlaceholder.style.display = "block";
  }

  cardGenerated = true;
}

photoInput.addEventListener("change", () => {
  const file = photoInput.files[0];

  if (!file) {
    return;
  }

  if (!file.type.startsWith("image/")) {
    alert("Please select a valid image.");
    photoInput.value = "";
    photoLabel.textContent = "Choose your photograph (optional)";
    return;
  }

  if (file.size > 5 * 1024 * 1024) {
    alert("Please choose an image smaller than 5 MB.");
    photoInput.value = "";
    photoLabel.textContent = "Choose your photograph (optional)";
    return;
  }

  if (selectedPhotoURL) {
    URL.revokeObjectURL(selectedPhotoURL);
  }

  selectedPhotoURL = URL.createObjectURL(file);
  photoLabel.textContent = file.name;
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();

  if (!form.reportValidity()) {
    return;
  }

  generateCard();
  const submitButton = form.querySelector('[type="submit"]');
  const originalButtonText = submitButton.textContent;
  submitButton.disabled = true;
  submitButton.textContent = "Saving...";

  try {
    const fields = new URLSearchParams({
      email: emailInput.value.trim(),
      name: nameInput.value.trim(),
      studentId: idInput.value.trim(),
      degree: degreeInput.value,
      department: deptInput.value,
      year: yearInput.value,
      academicYears: durationInput.value.trim()
    });
    const response = await fetch("/api/students", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: fields.toString()
    });
    if (response.status === 403) {
      throw new Error("Only a valid @srmap.edu.in email address is allowed.");
    }
    if (!response.ok) throw new Error("The server could not save this student record.");
    alert("Student ID card generated and student details saved.");
  } catch (error) {
    alert(`${error.message} Make sure the Java server is running.`);
    console.error("Student record save error:", error);
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = originalButtonText;
  }
});

form.addEventListener("reset", () => {
  setTimeout(() => {
    if (selectedPhotoURL) {
      URL.revokeObjectURL(selectedPhotoURL);
      selectedPhotoURL = null;
    }

    if (generatedPhotoURL) {
      URL.revokeObjectURL(generatedPhotoURL);
      generatedPhotoURL = null;
    }

    previewPhoto.removeAttribute("src");
    previewPhoto.style.display = "none";
    photoPlaceholder.style.display = "block";
    photoLabel.textContent = "Choose your photograph (optional)";

    [
      "previewName",
      "previewId",
      "previewDegree",
      "previewDept",
      "previewYear",
      "previewDuration"
    ].forEach((id) => {
      document.getElementById(id).textContent = "";
    });

    cardGenerated = false;
  }, 0);
});

document.getElementById("printButton").addEventListener("click", () => {
  if (!cardGenerated) {
    alert("Please complete the form and generate the card first.");
    return;
  }

  window.print();
});

document.getElementById("downloadButton").addEventListener(
  "click",
  async () => {
    if (!cardGenerated) {
      alert("Please generate the card before downloading.");
      return;
    }

    if (typeof html2canvas === "undefined") {
      alert("Image download needs an internet connection.");
      return;
    }

    try {
      const canvas = await html2canvas(
        document.getElementById("idCard"),
        {
          scale: 2,
          backgroundColor: "#ffffff"
        }
      );

      const link = document.createElement("a");
      link.download = "student-id-card-demo.png";
      link.href = canvas.toDataURL("image/png");
      link.click();
    } catch (error) {
      console.error("Download error:", error);
      alert("Unable to download the card. Please try again.");
    }
  }
);
