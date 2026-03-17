from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn

app = FastAPI()

class ExpenseRequest(BaseModel):
    description: str

@app.post("/predict")
def predict_category(request: ExpenseRequest):
    description = request.description.lower()
    
    # Comprehensive Local AI logic mapping description keywords to Categories
    if any(word in description for word in ["uber", "ola", "flight", "train", "bus", "petrol"]):
        category = "Travel"
    elif any(word in description for word in ["zomato", "swiggy", "kfc", "mcdonalds", "lunch", "dinner", "food", "grocery"]):
        category = "Food"
    elif any(word in description for word in ["amazon", "flipkart", "myntra", "clothes", "shoe", "shirt"]):
        category = "Shopping"
    elif any(word in description for word in ["netflix", "prime", "spotify", "movie", "ticket"]):
        category = "Entertainment"
    elif any(word in description for word in ["doctor", "medicine", "pharmacy", "hospital", "clinic"]):
        category = "Health"
    elif any(word in description for word in ["school", "college", "tuition", "course", "book"]):
        category = "Academic"
    elif any(word in description for word in ["rent", "lease", "deposit", "pg"]):
        category = "Rent"
    elif any(word in description for word in ["recharge", "jio", "airtel", "vi", "broadband"]):
        category = "Recharges"
    elif any(word in description for word in ["electricity", "water", "gas", "bill", "emi"]):
        category = "Bills"
    elif any(word in description for word in ["stock", "sip", "fd", "rd", "gold", "mutual"]):
        category = "Investment"
    else:
        category = "Other"

    return {"category": category}

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)
